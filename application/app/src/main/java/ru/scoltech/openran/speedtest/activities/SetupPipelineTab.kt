package ru.scoltech.openran.speedtest.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import ru.scoltech.openran.speedtest.R


class SetupPipelineTab : Fragment() {
    companion object {
        private val TAG = SetupPipelineTab::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_setup_pipeline, container, true)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        val iperfPref = activity.getSharedPreferences(
            getString(R.string.iperfSharedPreferences), AppCompatActivity.MODE_PRIVATE
        )
        val DOWNLOAD_DEVICE_IPERF_ARGS = iperfPref.getString(
            getString(R.string.download_device_args),
            getString(R.string.default_download_device_iperf_args)
        )
        val DOWNLOAD_SERVER_IPERF_ARGS = iperfPref.getString(
            getString(R.string.download_server_args),
            getString(R.string.default_download_server_iperf_args)
        )
        val UPLOAD_DEVICE_IPERF_ARGS = iperfPref.getString(
            getString(R.string.upload_device_args),
            getString(R.string.default_upload_device_iperf_args)
        )
        val UPLOAD_SERVER_IPERF_ARGS = iperfPref.getString(
            getString(R.string.upload_server_args),
            getString(R.string.default_upload_server_iperf_args)
        )
        val iperfUploadDevText = view.findViewById<EditText>(R.id.upload_device_args)
        iperfUploadDevText.setText(UPLOAD_DEVICE_IPERF_ARGS)
        iperfUploadDevText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val editor = activity.getSharedPreferences(
                    getString(R.string.iperfSharedPreferences),
                    AppCompatActivity.MODE_PRIVATE
                ).edit()
                editor.putString(getString(R.string.upload_device_args), s.toString())
                editor.apply()
                Log.d(TAG, "update UploadDeviceArgs = $s")
            }
        })
        val iperfUploadServText = view.findViewById<EditText>(R.id.upload_server_args)
        iperfUploadServText.setText(UPLOAD_SERVER_IPERF_ARGS)
        iperfUploadServText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val editor = activity.getSharedPreferences(
                    getString(R.string.iperfSharedPreferences),
                    AppCompatActivity.MODE_PRIVATE
                ).edit()
                editor.putString(getString(R.string.upload_server_args), s.toString())
                editor.apply()
                Log.d(TAG, "update UploadServerArgs = $s")
            }
        })

        val iperfDownloadDevText = view.findViewById<EditText>(R.id.download_device_args)
        iperfDownloadDevText.setText(DOWNLOAD_DEVICE_IPERF_ARGS)
        iperfDownloadDevText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val editor = activity.getSharedPreferences(
                    getString(R.string.iperfSharedPreferences),
                    AppCompatActivity.MODE_PRIVATE
                ).edit()
                editor.putString(getString(R.string.download_device_args), s.toString())
                editor.apply()
                Log.d(TAG, "update DownloadDeviceArgs = $s")
            }
        })
        val iperfDownloadServText = view.findViewById<EditText>(R.id.download_server_args)
        iperfDownloadServText.setText(DOWNLOAD_SERVER_IPERF_ARGS)
        iperfDownloadServText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val editor = activity.getSharedPreferences(
                    getString(R.string.iperfSharedPreferences),
                    AppCompatActivity.MODE_PRIVATE
                ).edit()
                editor.putString(getString(R.string.download_server_args), s.toString())
                editor.apply()
                Log.d(TAG, "update DownloadServerArgs = $s")
            }
        })


        val childCountPreferences = requireActivity().getSharedPreferences("pipeline_count",
            AppCompatActivity.MODE_PRIVATE)
        val childCount = childCountPreferences.getString("0", "0")
            .toString().toInt()

        val addPipelineButton = view.findViewById<Button>(R.id.add_pipeline_button)
        val pipelineLayout = view.findViewById<LinearLayout>(R.id.new_pipelines)
        val delButton = view.findViewById<Button>(R.id.deleteButton)


        println(childCount)
        for (index in 0 until childCount){
            val pipelineEditor = requireActivity().getSharedPreferences(
                "iperf_args_pipeline_$index",
                AppCompatActivity.MODE_PRIVATE).getString("0", "\n\n").toString()
                    .split('\n')
            println(pipelineEditor)
            LayoutInflater.from(requireContext()).inflate(R.layout.pipeline_sample, pipelineLayout)

            pipelineLayout.getChildAt(index).findViewById<EditText>(R.id.pipeline_name)
                    .setText(pipelineEditor[0])
            pipelineLayout.getChildAt(index).findViewById<EditText>(R.id.device_args)
                    .setText(pipelineEditor[1])
            pipelineLayout.getChildAt(index).findViewById<EditText>(R.id.server_args)
                    .setText(pipelineEditor[2])

            pipelineLayout.getChildAt(index).findViewById<TextView>(R.id.pipeline_name)
                .addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {}
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                        println(123)

                        requireActivity().getSharedPreferences(
                            "iperf_args_pipeline_${index}",
                            AppCompatActivity.MODE_PRIVATE
                        ).edit {
                            putString("0", serializePipeline(
                                pipelineLayout.getChildAt(index)))
                        }
                    }
                })
            pipelineLayout.getChildAt(index).findViewById<TextView>(R.id.device_args)
                .addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {}
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {


                        requireActivity().getSharedPreferences(
                            "iperf_args_pipeline_${index}",
                            AppCompatActivity.MODE_PRIVATE
                        ).edit {
                            putString("0", serializePipeline(
                                pipelineLayout.getChildAt(index)))
                        }
                    }
                })
            pipelineLayout.getChildAt(index).findViewById<TextView>(R.id.server_args)
                .addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {}
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {


                        requireActivity().getSharedPreferences(
                            "iperf_args_pipeline_${index}",
                            AppCompatActivity.MODE_PRIVATE
                        ).edit {
                            putString("0", serializePipeline(
                                pipelineLayout.getChildAt(index)))
                        }
                    }
                })
//            delButton.setOnClickListener {  removePipeline(pipelineLayout, index) }
        }


        addPipelineButton.setOnClickListener { newPipeline(pipelineLayout) }
    }


    private fun removePipeline(layout : View){
        layout
    }

    //    ((parent.getChildAt(0) as ViewGroup).children.toList()[2] as TextInputEditText)

//TODO: rewrite the code so it uses editor map as map, not string
    private fun serializePipeline(pipeline : View): String {
        val name = pipeline.findViewById<TextInputEditText>(R.id.pipeline_name).text.toString()
        val device = pipeline.findViewById<TextInputEditText>(R.id.device_args).text.toString()
        val server = pipeline.findViewById<TextInputEditText>(R.id.server_args).text.toString()
        println("$name\n$device\n$server".split('\n'))
        return "$name\n$device\n$server"
    }



    private fun newPipeline(parent: ViewGroup) {
        val pipelineForm = LayoutInflater.from(requireContext())
            .inflate(R.layout.pipeline_sample, parent)

        val childCountEditor = requireActivity().getSharedPreferences("pipeline_count",
            AppCompatActivity.MODE_PRIVATE)
        val childCount = childCountEditor.getString("0", "0")
            .toString().toInt()
        childCountEditor.edit {
            putString("0", (childCount + 1).toString())
        }

        parent.getChildAt(childCount).findViewById<EditText>(R.id.pipeline_name)
            .addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {


                    requireActivity().getSharedPreferences(
                        "iperf_args_pipeline_${childCount + 1}",
                        AppCompatActivity.MODE_PRIVATE
                    ).edit {
                        putString("0", serializePipeline(
                            pipelineForm))
                    }
                }
            })

        parent.getChildAt(childCount).findViewById<EditText>(R.id.device_args)
            .addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {


                    requireActivity().getSharedPreferences(
                        "iperf_args_pipeline_${childCount + 1}",
                        AppCompatActivity.MODE_PRIVATE
                    ).edit {
                        putString("0", serializePipeline(
                            pipelineForm))
                    }
                }
            })

        parent.getChildAt(childCount).findViewById<EditText>(R.id.server_args)
            .addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {


                    requireActivity().getSharedPreferences(
                        "iperf_args_pipeline_${childCount + 1}",
                        AppCompatActivity.MODE_PRIVATE
                    ).edit {
                        putString("0", serializePipeline(
                            pipelineForm))
                    }
                }
            })

            requireActivity().getSharedPreferences(
                "iperf_args_pipeline_${childCount + 1}",
                AppCompatActivity.MODE_PRIVATE
            ).edit {
                putString("0", serializePipeline(
                    pipelineForm))
            }

        val a = 5


    }

}
