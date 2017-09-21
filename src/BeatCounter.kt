import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import be.tarsos.dsp.util.BiQuadFilter

class BeatCounter {

    companion object {
        private val MIN_BPM = 90
        private val MAX_BPM = 180

        fun getBPM(path: String): Float {
            val countBpmDispatcher = AudioDispatcherFactory.fromPipe(path, 44100, 4096, 0, 0.0, 60.0)
            val bpmProcessor = BPMProcessor()
            countBpmDispatcher.addAudioProcessor(bpmProcessor)
            countBpmDispatcher.run()
            return bpmProcessor.bpm
        }

        fun getBPM(audio: FloatArray): Float {
            return countBpm(doFilteringForOneChannel(audio))
        }

        private fun doFilteringForOneChannel(audio: FloatArray): FloatArray {
            val lowPass = BiQuadFilter(1.12965674E-4, 2.2593135E-4, 1.12965674E-4, -1.9784044, 0.9788562)
            val highPass = BiQuadFilter(0.99287635, -1.9857527, 0.99287635, -1.985652, 0.9858537)
            val tmp = FloatArray(audio.size)
            lowPass.doFiltering(audio, tmp)
            highPass.doFiltering(audio, tmp)
            return audio
        }

        private fun doFilteringForTwoChannels(audio: Array<FloatArray>): Array<FloatArray> {
            audio[0] = doFilteringForOneChannel(audio[0])
            audio[1] = doFilteringForOneChannel(audio[1])
            return audio
        }

        private fun parseChannel(audio: FloatArray): MutableList<BeatInfo> {
            var beats = mutableListOf<BeatInfo>()
            run {
                val i = audio.size / 22050
                var j = 0
                while (j < i) {
                    var beatInfo: BeatInfo? = null
                    for (position in 22050 * j until 22050 * (j + 1)) {
                        val vol = Math.abs(audio[position])
                        if (beatInfo == null || vol > beatInfo.volume) {
                            beatInfo = BeatInfo(position, vol)
                        }
                    }
                    beats.add(beatInfo!!)
                    j++
                }
            }
            beats.sortByDescending { o -> o.volume }
            beats = beats.subList(0, beats.size / 2)
            beats.sortBy { b -> b.position }
            return beats
        }

        private fun parseChannels(audio: Array<FloatArray>): MutableList<BeatInfo> {
            val left = audio[0]
            val right = audio[1]
            var beats = mutableListOf<BeatInfo>()
            run {
                val i = left.size / 22050
                var j = 0
                while (j < i) {
                    var beatInfo: BeatInfo? = null
                    for (position in 22050 * j until 22050 * (j + 1)) {
                        val vol = Math.max(Math.abs(left[position]),
                                Math.abs(right[position]))
                        if (beatInfo == null || vol > beatInfo.volume) {
                            beatInfo = BeatInfo(position, vol)
                        }
                    }
                    beats.add(beatInfo!!)
                    j++
                }
            }
            beats.sortByDescending { o -> o.volume }
            beats = beats.subList(0, beats.size / 2)
            beats.sortBy { b -> b.position }
            return beats
        }

        private fun doCounting(beats: MutableList<BeatInfo>): Float {
            val allBpm = mutableListOf<BPMInfo>()
            for (idx in beats.indices) {
                val beat = beats[idx]
                var i = 1
                while (idx + i < beats.size && 10 > i) {
                    val bpmInfo = BPMInfo(2646000f / (beats[idx + i].position - beat.position),
                            1, beat.volume, beat.position)
                    while (bpmInfo.bpm < MIN_BPM) {
                        bpmInfo.bpm *= 2f
                    }
                    while (bpmInfo.bpm > MAX_BPM) {
                        bpmInfo.bpm /= 2f
                    }
                    bpmInfo.bpm = Math.round(bpmInfo.bpm).toFloat()
                    var flag = true
                    for (b in allBpm) {
                        if (b.bpm == bpmInfo.bpm) {
                            b.count++
                            flag = false
                        }
                    }
                    if (flag) {
                        allBpm.add(bpmInfo)
                    }
                    i++
                }
            }
            allBpm.sortByDescending { o -> o.count }
            println(allBpm[0])
            return allBpm[0].bpm
        }

        private fun countBpm(audio: Array<FloatArray>): Float {
            val beats = parseChannels(audio)
            return doCounting(beats)
        }

        private fun countBpm(audio: FloatArray): Float {
            val beats = parseChannel(audio)
            return doCounting(beats)
        }
    }
}