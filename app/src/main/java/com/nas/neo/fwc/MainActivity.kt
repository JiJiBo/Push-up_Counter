/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nas.neo.fwc

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.nas.neo.fwc.databinding.ActivityMainBinding
import com.nas.neo.fwc.utils.FWC_manager
import com.nas.neo.fwc.utils.SpeechUtils
import com.rulerbug.bugutils.Utils.BugSpUtils

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.let { FWC_manager.setContext(it) }
        SpeechUtils.getInstance(this).init()
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(activityMainBinding.root)
        if (!BugSpUtils.getSpBoolean("isFirstTime", true)) {
            activityMainBinding.rlAlert.visibility = View.GONE
        }
        activityMainBinding.rlAlert.setOnClickListener {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("提示")
                .setMessage("确定要隐藏此提示并不再显示吗？")
                .setPositiveButton("确定") { dialog, _ ->
                    // 用户确认，保存状态并隐藏
                    BugSpUtils.putSpBoolean("isFirstTime", false)
                    activityMainBinding.rlAlert.visibility = View.GONE
                    dialog.dismiss()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    // 用户取消，仅关闭对话框
                    dialog.dismiss()
                }
                .create()
                .show()
        }

    }

    override fun onBackPressed() {
        finish()
    }
}