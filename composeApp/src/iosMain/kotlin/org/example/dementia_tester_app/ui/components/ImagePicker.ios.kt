package org.example.dementia_tester_app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray) -> Unit
): () -> Unit {

    val viewController =
        LocalUIViewController.current

    val delegate =
        remember {
            object :
                NSObject(),
                PHPickerViewControllerDelegateProtocol {

                override fun picker(
                    picker: PHPickerViewController,
                    didFinishPicking: List<*>
                ) {
                    picker.dismissViewControllerAnimated(
                        true,
                        null
                    )

                    val result =
                        didFinishPicking.firstOrNull()
                                as? PHPickerResult
                            ?: return

                    val itemProvider =
                        result.itemProvider

                    val imageTypeIdentifier =
                        UTTypeImage.identifier

                    if (
                        !itemProvider.hasItemConformingToTypeIdentifier(
                            imageTypeIdentifier
                        )
                    ) {
                        return
                    }

                    itemProvider.loadDataRepresentationForTypeIdentifier(
                        typeIdentifier =
                            imageTypeIdentifier,
                        completionHandler = {
                                data: NSData?,
                                error ->

                            if (
                                error != null ||
                                data == null
                            ) {
                                return@loadDataRepresentationForTypeIdentifier
                            }

                            val length =
                                data.length.toInt()

                            if (length <= 0) {
                                return@loadDataRepresentationForTypeIdentifier
                            }

                            val bytes =
                                data.bytes
                                    ?.readBytes(
                                        length
                                    )
                                    ?: return@loadDataRepresentationForTypeIdentifier

                            onImagePicked(
                                bytes
                            )
                        }
                    )
                }
            }
        }

    return {
        val configuration =
            PHPickerConfiguration()

        configuration.filter =
            PHPickerFilter.imagesFilter()

        configuration.selectionLimit =
            1

        val picker =
            PHPickerViewController(
                configuration
            )

        picker.delegate =
            delegate

        viewController.presentViewController(
            picker,
            animated = true,
            completion = null
        )
    }
}