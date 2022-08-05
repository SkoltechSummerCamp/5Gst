package ru.scoltech.openran.speedtest.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import ru.scoltech.openran.speedtest.R
import java.io.File


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


    @SuppressLint("CommitPrefEdits")
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

        val pipelineEditor = requireActivity().getSharedPreferences(
            "iperf_args_pipeline",
            AppCompatActivity.MODE_PRIVATE)
        println(childCount)
        var pipelineList = mutableListOf<Int>()
        if (childCountPreferences.getString("1", "").toString() != ""){
            pipelineList = childCountPreferences.getString("1", "").toString()
                .split(' ').map { it.toInt() }.toMutableList()
        }
        /*<string name="0">3</string>
        <string name="1">012</string>*/
        /*<string name="1">123&#10;werh&#10;    </string>*/
        for (index in pipelineList){


            LayoutInflater.from(requireContext()).inflate(R.layout.pipeline_sample, pipelineLayout)

            val pipelineConfig = pipelineEditor.getString("$index", "\n\n").toString()
                .split('\n')
            println(pipelineEditor)



            print("index = $index")
            pipelineLayout.getChildAt(index).findViewById<EditText>(R.id.pipeline_name)
                    .setText(pipelineConfig[0])
            pipelineLayout.getChildAt(index).findViewById<EditText>(R.id.device_args)
                    .setText(pipelineConfig[1])
            pipelineLayout.getChildAt(index).findViewById<EditText>(R.id.server_args)
                    .setText(pipelineConfig[2])

            pipelineLayout.getChildAt(index).findViewById<TextView>(R.id.pipeline_name)
                .addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {}
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                        println(123)

                        requireActivity().getSharedPreferences(
                            "iperf_args_pipeline",
                            AppCompatActivity.MODE_PRIVATE
                        ).edit {
                            putString("$index", serializePipeline(
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
                            "iperf_args_pipeline",
                            AppCompatActivity.MODE_PRIVATE
                        ).edit {
                            putString("$index", serializePipeline(
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
                            "iperf_args_pipeline",
                            AppCompatActivity.MODE_PRIVATE
                        ).edit {
                            putString("$index", serializePipeline(
                                pipelineLayout.getChildAt(index)))
                        }
                    }
                })
            pipelineLayout.getChildAt(index).findViewById<ImageButton>(R.id.deleteButton)
                .setOnClickListener {
                    pipelineEditor.edit().remove("$index").apply()
                    println("remove $index")
                    pipelineList.remove(index)
                    pipelineLayout.removeViewAt(index)
                }
        }


        addPipelineButton.setOnClickListener { newPipeline(pipelineLayout) }
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
        var newList =
            "${childCountEditor.getString("1", "")} " +
                    "${childCountEditor.getString("0", "0").toString().toInt()}"
        if (newList[0] == ' ')
            newList = newList.drop(1)
        childCountEditor.edit(){putString("1", newList)}.apply{}
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
                        "iperf_args_pipeline",
                        AppCompatActivity.MODE_PRIVATE
                    ).edit {
                        putString("${childCount + 1}", serializePipeline(
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
                        "iperf_args_pipeline",
                        AppCompatActivity.MODE_PRIVATE
                    ).edit {
                        putString("${childCount + 1}", serializePipeline(
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
                        "iperf_args_pipeline",
                        AppCompatActivity.MODE_PRIVATE
                    ).edit {
                        putString("${childCount + 1}", serializePipeline(
                            pipelineForm))
                    }
                }
            })

            requireActivity().getSharedPreferences(
                "iperf_args_pipeline",
                AppCompatActivity.MODE_PRIVATE
            ).edit {
                putString("${childCount + 1}", serializePipeline(
                    pipelineForm))
            }
/*
        parent.getChildAt(index).findViewById<ImageButton>(R.id.deleteButton)
            .setOnClickListener {
                pipelineEditor.edit().remove("$index").apply()
                println("remove $index")
                pipelineList.remove(index)
                pipelineLayout.removeViewAt(index)
            }*/
    }

}
