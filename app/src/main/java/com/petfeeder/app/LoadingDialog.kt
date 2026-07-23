package com.petfeeder.app

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

class LoadingDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_loading, null)

        val ivPaw = view.findViewById<ImageView>(R.id.ivPawLoading)
        val pulseAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.paw_loading)
        ivPaw.startAnimation(pulseAnim)

        val texto = arguments?.getString(ARG_TEXTO) ?: "Cargando..."
        view.findViewById<TextView>(R.id.tvLoadingText).text = texto

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.4f)
        }

        return dialog
    }

    companion object {
        private const val TAG = "pf_loading"
        private const val ARG_TEXTO = "texto"

        fun show(manager: FragmentManager, texto: String = "Cargando...") {
            try {
                if (manager.isStateSaved) return
                val existing = manager.findFragmentByTag(TAG)
                if (existing != null) return
                val dialog = LoadingDialog().apply {
                    arguments = Bundle().apply { putString(ARG_TEXTO, texto) }
                }
                dialog.show(manager, TAG)
            } catch (_: Exception) {}
        }

        fun dismiss(manager: FragmentManager) {
            try {
                val fragment = manager.findFragmentByTag(TAG) as? LoadingDialog
                fragment?.dismissAllowingStateLoss()
            } catch (_: Exception) {}
        }
    }
}
