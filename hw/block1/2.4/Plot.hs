{-# LANGUAGE ImportQualifiedPost #-}
{-# LANGUAGE ScopedTypeVariables #-}
{-# OPTIONS_GHC -Wno-name-shadowing #-}
{-# OPTIONS_GHC -Wno-unused-do-bind #-}

import Async
import Control.Applicative
import Data.Attoparsec.Text
import Data.Bifunctor
import Data.Char
import Data.Foldable
import Data.Functor hiding (unzip)
import Data.List qualified as L
import Data.Map qualified as M
import Data.Text qualified as T
import Data.Text.IO qualified
import Graphics.Rendering.Chart.Backend.Diagrams
import Graphics.Rendering.Chart.Easy hiding ((<.>))
import System.Directory
import System.FilePath

data BenchmarkResult = BenchmarkResult {operation :: Op, threadCount :: Int, score :: Double, scoreError :: Double}

comma :: Parser ()
comma = void $ char ','

data Op = Get | Inc deriving (Eq)

parseLine :: Parser (String, BenchmarkResult)
parseLine = do
  benchmarkName <- T.unpack <$> parseBenchmarkName
  comma
  skipField
  comma
  threads <- decimal
  comma
  skipField
  comma
  score <- double
  comma
  scoreError <- double
  comma
  skipField
  comma
  counter <- T.unpack <$> takeWhile1 identChar
  let op = (if "get" `L.isSubsequenceOf` benchmarkName then Get else Inc)
  pure (counter, BenchmarkResult op threads score scoreError)
  where
    skipField = takeWhile1 (/= ',')
    parseBenchmarkName = do
      char '"'
      name <- last <$> sepBy1 (takeWhile1 identChar) (char '.')
      char '"'
      pure name
    identChar c = isAlphaNum c || c == '_'

parseBenchmarkData :: Parser (M.Map String [BenchmarkResult])
parseBenchmarkData = do
  manyTill anyChar endOfLine
  foldl (\m (name, res) -> updateOrInsert (maybe [res] (res :)) name m) M.empty <$> many (skipSpace *> parseLine)
  where
    updateOrInsert :: (Ord k) => (Maybe v -> v) -> k -> M.Map k v -> M.Map k v
    updateOrInsert inserter key m = M.insert key (inserter $ M.lookup key m) m

inputFile :: FilePath
inputFile = "jmh-result.csv"

plotsDir :: FilePath
plotsDir = "plots"

main :: IO ()
main = do
  input <- Data.Text.IO.readFile inputFile
  either (putStrLn . ("error occured: " ++)) makePlots (parseOnly parseBenchmarkData input)

makePlots :: M.Map String [BenchmarkResult] -> IO ()
makePlots benchmarkData = do
  createDirectoryIfMissing False plotsDir
  let results = (\name -> (name, benchmarkData M.! name)) <$> M.keys benchmarkData
  runAsync_ $ do
    for_
      results
      (\(benchName, results) -> runTask_ $ plotter benchName (tupleToList $ toCorrelations results))
    let splits = filter (("Split" `L.isSubsequenceOf`) . fst) results
    runTask_ $
      plotter "SplitCounter_get" (mergeSplits $ second (filter ((== Get) . operation)) <$> splits)
    runTask_ $
      plotter "SplitCounter_increment" (mergeSplits $ second (filter ((== Inc) . operation)) <$> splits)

mergeSplits :: [(String, [BenchmarkResult])] -> [Correlation]
mergeSplits = fmap (uncurry correlations)
  where
    correlations :: String -> [BenchmarkResult] -> Correlation
    correlations splitName results = Correlation splitName (fmap resToPoint results)

resToPoint :: BenchmarkResult -> (Int, Double, Double)
resToPoint r = (threadCount r, score r, scoreError r)

toCorrelations :: [BenchmarkResult] -> (Correlation, Correlation)
toCorrelations results = (Correlation "get" (resToPoint <$> gets), Correlation "inc" (resToPoint <$> incs))
  where
    (gets, incs) = partitionOnOp results
    partitionOnOp = L.partition ((== Get) . operation)

tupleToList :: (a, a) -> [a]
tupleToList (x, y) = [x, y]

data Correlation = Correlation String [(Int, Double, Double)] deriving (Show)

plotter :: String -> [Correlation] -> IO ()
plotter benchmarkName results = do
  putStrLn ("plotting " ++ benchmarkName)
  toFile def (plotsDir </> benchmarkName <.> "svg") ec
  putStrLn ("finished " ++ benchmarkName)
  where
    ec :: EC (Layout Int Double) ()
    ec = do
      setColors [opaque green, opaque red, opaque magenta, opaque orange, opaque coral, opaque darkviolet]
      layout_title .= benchmarkName
      layout_x_axis . laxis_title .= "threads"
      layout_y_axis . laxis_title .= "ops/ms"
      plot $ errbars "Score error (99.9%)" (results >>= (\(Correlation _ vals) -> vals))
      for_ results (\(Correlation name points) -> plot $ line name [(\(t, s, _) -> (t, s)) <$> points])

errbars :: (Num x, Num y) => String -> [(x, y, y)] -> EC l (PlotErrBars x y)
errbars title vals = liftEC $ do
  let c = opaque blue
  plot_errbars_values .= [symErrPoint x y 0 dy | (x, y, dy) <- vals]
  plot_errbars_title .= title
  plot_errbars_line_style . line_color .= c
