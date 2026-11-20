package xyz.emlyn.patch.instagram.qbf

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.copyResources
import app.morphe.util.ResourceGroup

val qbfResourcesPatch = resourcePatch {
    execute {
        copyResources(
            "qbf",
            ResourceGroup(
                "drawable",
                "ic_lens_instagram.xml",
                "ic_lens_ring_instagram.xml",
                "ic_lens_shield_instagram.xml"
            ),
            ResourceGroup(
                "drawable-night",
                "ic_lens_instagram.xml",
                "ic_lens_ring_instagram.xml",
                "ic_lens_shield_instagram.xml"
            ),
        )
    }
}
