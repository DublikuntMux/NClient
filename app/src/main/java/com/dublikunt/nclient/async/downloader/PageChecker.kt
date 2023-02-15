package com.dublikunt.nclient.async.downloader

class PageChecker : Thread() {
    override fun run() {
        for (g in DownloadQueue.downloaders) if (g!!.hasData()) g.initDownload()
    }
}
