fun main(args: Array<String>) {
    val path = "/Users/RexZhang/Desktop/test.m4a"
    val beatDetector = BeatDetector()
    beatDetector.getBPM(path)
}
