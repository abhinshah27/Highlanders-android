/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.utility

import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieListener
import rs.highlande.highlanders_app.base.HLApp

abstract class LottieCompositioListener: LottieListener<LottieComposition> {

    override fun onResult(result: LottieComposition?) {
        HLApp.siriComposition = result
    }
}