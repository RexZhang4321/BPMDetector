import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.GainProcessor
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import be.tarsos.dsp.io.jvm.AudioPlayer

fun main(args: Array<String>) {
    val path = "/Users/RexZhang/Desktop/test.m4a"
    val gain = GainProcessor(1.0)
    val wsola = WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.slowdownDefaults(1.0, 44100.0))
    System.out.println(BeatCounter.getBPM(path))
    val dispatcher: AudioDispatcher = AudioDispatcherFactory.fromPipe(path, 44100, wsola.inputBufferSize, wsola.overlap)
    val audioFormat = dispatcher.format
    val audioPlayer = AudioPlayer(audioFormat)
    wsola.setDispatcher(dispatcher)
    dispatcher.addAudioProcessor(wsola)
    dispatcher.addAudioProcessor(gain)
    dispatcher.addAudioProcessor(audioPlayer)
    val t = Thread(dispatcher)
    t.start()
    while (true) {
        val newTempo = readLine()!!.toDouble()
        wsola.setParameters(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(newTempo, audioFormat.sampleRate.toDouble()))
    }
}
