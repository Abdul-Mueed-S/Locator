package com.evu.locator.ui.traveler

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.evu.locator.databinding.DialogQrBinding
import com.evu.locator.qr.QrCodeGenerator
import java.io.File
import java.io.FileOutputStream

class QrDialogFragment(private val myUid: String) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogQrBinding.inflate(layoutInflater)
        val bitmap = QrCodeGenerator.generate(myUid)
        binding.qrImage.setImageBitmap(bitmap)

        binding.shareBtn.setOnClickListener {
            shareQr(bitmap)
        }
        binding.closeBtn.setOnClickListener {
            dismiss()
        }

        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun shareQr(bitmap: Bitmap) {
        val cachePath = File(requireContext().cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "qr_share.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share QR code"))
    }
}