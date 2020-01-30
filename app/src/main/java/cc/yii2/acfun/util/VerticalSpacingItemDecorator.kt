package cc.yii2.acfun.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView


class VerticalSpacingItemDecorator(private val verticalSpaceHeight: Int) :
    RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // todo: do we need parent's call?
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = verticalSpaceHeight
    }
}