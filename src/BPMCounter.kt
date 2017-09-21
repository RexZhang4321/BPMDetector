import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

class BPMCounter : AudioProcessor{
    private val audio: MutableList<Float> = ArrayList<Float>()

    override fun processingFinished() {
        BeatDetector.getBPM(audio.toFloatArray())
    }

    override fun process(audioEvent: AudioEvent?): Boolean {
        audio.addAll(audioEvent!!.floatBuffer.toList())
        return true
    }
}