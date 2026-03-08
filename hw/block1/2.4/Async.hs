{-# LANGUAGE GADTs #-}
{-# OPTIONS_GHC -Wno-unused-do-bind #-}

module Async
  ( Async,
    Future,
    runAsync,
    runAsync_,
    runTask,
    runTask_,
    await,
  )
where

import Control.Concurrent
import Control.Monad
import Control.Monad.IO.Class

newtype Async a = Async (ThreadPool -> IO a)

instance Functor Async where
  fmap f (Async c) = Async (fmap f . c)

instance Applicative Async where
  pure = Async . const . pure
  (Async f) <*> (Async g) = Async $ \t -> f t <*> g t

instance Monad Async where
  (Async c) >>= f = Async $ \t -> do
    x <- c t
    let (Async c2) = f x
    c2 t

instance MonadIO Async where
  liftIO = Async . const . liftIO

data Tasks where
  Nil :: Tasks
  Cons :: Future a -> Tasks -> Tasks

newtype ThreadPool = ThreadPool (MVar Tasks)

newtype Future a = Future (MVar (Maybe a))

awaitIO :: Future a -> IO (Maybe a)
awaitIO (Future mvar) = readMVar mvar

await :: Future a -> Async (Maybe a)
await = liftIO . awaitIO

threadPool :: IO ThreadPool
threadPool = ThreadPool <$> newMVar Nil

stopThreadPool :: MVar Tasks -> IO ()
stopThreadPool threads = do
  tasks <- liftIO $ takeMVar threads
  case tasks of
    Nil -> pure ()
    Cons t ts -> do
      liftIO $ putMVar threads ts
      awaitIO t
      stopThreadPool threads

runTask :: IO a -> Async (Future a)
runTask task =
  Async $ \(ThreadPool tasks) -> liftIO $ do
    future@(Future mvar) <- Future <$> newEmptyMVar
    liftIO $ modifyMVar_ tasks (\ts -> pure (future `Cons` ts))
    forkFinally
      (task >>= (putMVar mvar . Just))
      (\_ -> putMVar mvar Nothing)
    pure future

runTask_ :: IO a -> Async (Future ())
runTask_ = runTask . void

runAsync :: Async a -> IO a
runAsync (Async comp) = do
  tp@(ThreadPool threads) <- threadPool
  res <- comp tp
  stopThreadPool threads
  pure res

runAsync_ :: Async a -> IO ()
runAsync_ = void . runAsync
