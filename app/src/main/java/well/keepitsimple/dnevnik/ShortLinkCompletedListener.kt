package well.keepitsimple.dnevnik

import com.google.firebase.dynamiclinks.ShortDynamicLink

interface ShortLinkCompletedListener {
    fun onCompleted(link: ShortDynamicLink)
}