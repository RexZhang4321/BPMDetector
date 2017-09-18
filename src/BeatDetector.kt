import be.tarsos.dsp.util.BiQuadFilter

class BeatDetector {
    private val MIN_BPM = 90
    private val MAX_BPM = 180

    fun getBPM(path: String): Float {
        try {
            val aacFileReader = AACFileReader()
            val audio = aacFileReader.readFile(path)
            doFiltering(audio)
            return countBpm(audio)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0f
    }

    private fun doFiltering(audio: Array<FloatArray>): Array<FloatArray> {
        val lowPass = BiQuadFilter(1.12965674E-4, 2.2593135E-4, 1.12965674E-4, -1.9784044, 0.9788562)
        val highPass = BiQuadFilter(0.99287635, -1.9857527, 0.99287635, -1.985652, 0.9858537)
        val tmp = FloatArray(audio[0].size)
        lowPass.doFiltering(audio[0], tmp)
        lowPass.doFiltering(audio[1], tmp)
        highPass.doFiltering(audio[0], tmp)
        highPass.doFiltering(audio[1], tmp)
        return audio
    }

    private fun countBpm(audio: Array<FloatArray>): Float {
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
        val allBpm = mutableListOf<BpmInfo>()
        for (idx in beats.indices) {
            val beat = beats[idx]
            var i = 1
            while (idx + i < beats.size && 10 > i) {
                val bpmInfo = BpmInfo(2646000f / (beats[idx + i].position - beat.position),
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
}