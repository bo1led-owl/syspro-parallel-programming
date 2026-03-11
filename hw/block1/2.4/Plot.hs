{-# LANGUAGE ImportQualifiedPost #-}
{-# LANGUAGE ScopedTypeVariables #-}
{-# LANGUAGE TupleSections #-}
{-# OPTIONS_GHC -Wno-missing-fields #-}
{-# OPTIONS_GHC -Wno-name-shadowing #-}
{-# OPTIONS_GHC -Wno-unused-do-bind #-}

import Async
import Control.Applicative
import Data.Attoparsec.Text
import Data.Bifunctor
import Data.Char
import Data.Foldable
import Data.Function
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
makePlots unsortedData = do
  let benchmarkData = L.sortBy (compare `on` threadCount) <$> unsortedData
  createDirectoryIfMissing False plotsDir
  let results = (\name -> (name, benchmarkData M.! name)) <$> M.keys benchmarkData
  runAsync_ $ do
    for_ results (async_ . uncurry makePlot)
    let splits = filter (("Split" `L.isSubsequenceOf`) . fst) results
    async_ $ plotter "SplitCounter_get" (mergeSplits $ filterSplits Get <$> splits)
    async_ $ plotter "SplitCounter_increment" (mergeSplits $ filterSplits Inc <$> splits)
  where
    makePlot benchName results =
      plotter benchName ((tupleToList . toCorrelations) results)
    filterSplits op = second (filter ((== op) . operation))

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
  renderableToFile def (plotsDir </> benchmarkName <.> "svg") chart
  putStrLn ("finished " ++ benchmarkName)
  where
    chart = toRenderable layout
    layout =
      layout_title .~ benchmarkName $
        layout_x_axis . laxis_generate .~ tickedAxis $
          layout_x_axis . laxis_title .~ "threads" $
            layout_y_axis . laxis_title .~ "ops/ms" $
              layout_plots .~ (toPlot errbars : fmap toPlot benchmarkPlots) $
                def
    errbars =
      let vals = (results >>= (\(Correlation _ vals) -> vals))
       in plot_errbars_values .~ [symErrPoint x y 0 dy | (x, y, dy) <- vals] $
            plot_errbars_title .~ "Score error (99.9%)" $
              plot_errbars_line_style . line_color .~ opaque blue $
                def
    benchmarkPlots =
      zipWith
        makePoints
        ([opaque green, opaque red, opaque magenta, opaque orange, opaque coral, opaque darkviolet] :: [AlphaColour Double])
        results
    makePoints color (Correlation name points) =
      plot_lines_title .~ name $
        plot_lines_style . line_color .~ color $
          plot_lines_values .~ [(\(t, s, _) -> (t, s)) <$> points] $
            def

tickedAxis :: (Show a, Integral a) => [a] -> AxisData a
tickedAxis points = AxisData def vport invport ((,5) <$> points) [(\x -> (x, show x)) <$> points] points
  where
    vport r i = linMap id (fromIntegral imin - 0.5, fromIntegral imax + 0.5) r (fromIntegral i)
    invport = invLinMap round fromIntegral (imin, imax)
    imin = minimum points
    imax = maximum points
