package xyz.emlyn.patch.twitter.qbf

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.copyResources
import app.morphe.util.ResourceGroup

val qbfResourcesPatch = resourcePatch {
    execute {
        copyResources(
            "qbf", ResourceGroup("drawable", "qbf_icon.xml"),
        )
    }
}
