package com.luck.picture.lib

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.viewModelScope
import com.luck.picture.lib.base.BaseSelectorFragment
import com.luck.picture.lib.constant.SelectedState
import com.luck.picture.lib.constant.SelectorConstant
import com.luck.picture.lib.interfaces.OnRequestPermissionListener
import com.luck.picture.lib.media.ScanListener
import com.luck.picture.lib.permissions.OnPermissionResultListener
import com.luck.picture.lib.permissions.PermissionChecker
import com.luck.picture.lib.provider.TempDataProvider
import com.luck.picture.lib.utils.MediaUtils
import com.luck.picture.lib.utils.SdkVersionUtils.isQ
import com.luck.picture.lib.utils.ToastUtils
import kotlinx.coroutines.launch

/**
 * @author：luck
 * @date：2021/11/22 2:26 下午
 * @describe：SelectorCameraFragment
 */
open class SelectorCameraFragment : BaseSelectorFragment() {

    override fun getFragmentTag(): String {
        return SelectorCameraFragment::class.java.simpleName
    }

    override fun getResourceId(): Int {
        return R.layout.ps_empty
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            if (isQ()) {
                openSelectedCamera()
            } else {
                val permission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                PermissionChecker.requestPermissions(
                    this, permission,
                    object : OnPermissionResultListener {
                        override fun onGranted() {
                            showPermissionDescription(false, permission)
                            openSelectedCamera()
                        }

                        override fun onDenied() {
                            handlePermissionDenied(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        }
                    })
            }
        }
    }

    override fun showCustomPermissionApply(permission: Array<String>) {
        config.mListenerInfo.onPermissionApplyListener?.requestPermission(
            this,
            permission,
            object :
                OnRequestPermissionListener {
                override fun onCall(permission: Array<String>, isResult: Boolean) {
                    if (isResult) {
                        showPermissionDescription(false, permission)
                        openSelectedCamera()
                    } else {
                        handlePermissionDenied(permission)
                    }
                }
            })
    }

    override fun onResultCanceled(requestCode: Int, resultCode: Int) {
        if (resultCode == Activity.RESULT_CANCELED) {
            if (requestCode == SelectorConstant.REQUEST_GO_SETTING) {
                handlePermissionSettingResult(TempDataProvider.getInstance().currentRequestPermission)
            } else {
                onBackPressed()
            }
        }
    }

    override fun analysisCameraData(uri: Uri) {
        val context = requireContext()
        val isContent = uri.scheme.equals("content")
        val realPath = if (isContent) {
            MediaUtils.getPath(context, uri)
        } else {
            uri.path
        }
        if (TextUtils.isEmpty(realPath)) {
            return
        }
        viewModel.scanFile(if (isContent) realPath else null, object : ScanListener {
            override fun onScanFinish() {
                viewModel.viewModelScope.launch {
                    val media = if (isContent) {
                        MediaUtils.getAssignPathMedia(context, realPath!!)
                    } else {
                        MediaUtils.getAssignFileMedia(context, realPath!!)
                    }
                    if (confirmSelect(media, false) == SelectedState.SUCCESS) {
                        handleSelectResult()
                    } else {
                        onBackPressed()
                    }
                }
            }
        })
    }


    override fun handlePermissionSettingResult(permission: Array<String>) {
        if (permission.isEmpty()) {
            return
        }
        val context = requireContext()
        showPermissionDescription(false, permission)
        var isHasPermissions: Boolean
        val onPermissionApplyListener = config.mListenerInfo.onPermissionApplyListener
        if (onPermissionApplyListener != null) {
            isHasPermissions = onPermissionApplyListener.hasPermissions(this, permission)
        } else {
            isHasPermissions =
                PermissionChecker.checkSelfPermission(context, arrayOf(Manifest.permission.CAMERA))
            if (isQ()) {
            } else {
                isHasPermissions = PermissionChecker.checkSelfPermission(
                    context,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                )
            }
        }
        if (isHasPermissions) {
            openSelectedCamera()
        } else {
            if (!PermissionChecker.checkSelfPermission(
                    context,
                    arrayOf(Manifest.permission.CAMERA)
                )
            ) {
                ToastUtils.showMsg(context, getString(R.string.ps_camera))
            } else {
                if (!PermissionChecker.checkSelfPermission(
                        context,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    )
                ) {
                    ToastUtils.showMsg(requireContext(), getString(R.string.ps_jurisdiction))
                }
            }
            onBackPressed()
        }
        TempDataProvider.getInstance().currentRequestPermission = arrayOf()
    }
}