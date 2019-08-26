package com.fourcode.tracking.ui.navigation

import io.github.centrifugal.centrifuge.*
import timber.log.Timber


class CentrifugeListener: EventListener() {

    override fun onConnect(client: Client?, event: ConnectEvent?) {
        Timber.i("Centrifuge connected")
    }

}