package com.hannto.ippdemo;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hp.jipp.encoding.AttributeGroup;
import com.hp.jipp.encoding.AttributeType;
import com.hp.jipp.encoding.IppPacket;
import com.hp.jipp.encoding.Resolution;
import com.hp.jipp.encoding.ResolutionUnit;
import com.hp.jipp.model.Operation;
import com.hp.jipp.trans.IppClientTransport;
import com.hp.jipp.trans.IppPacketData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static com.hp.jipp.encoding.AttributeGroup.groupOf;
import static com.hp.jipp.encoding.Tag.operationAttributes;
import static com.hp.jipp.model.Types.attributesCharset;
import static com.hp.jipp.model.Types.attributesNaturalLanguage;
import static com.hp.jipp.model.Types.copies;
import static com.hp.jipp.model.Types.documentFormat;
import static com.hp.jipp.model.Types.jobCopies;
import static com.hp.jipp.model.Types.jobId;
import static com.hp.jipp.model.Types.lastDocument;
import static com.hp.jipp.model.Types.pclmSourceResolution;
import static com.hp.jipp.model.Types.printColorMode;
import static com.hp.jipp.model.Types.printerUri;
import static com.hp.jipp.model.Types.requestingUserName;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private final static String TAG = "ipp_demo";

    private TextView textView;
    private Button button,sendFileButton,getJobButton, pclmTestButton, pwgSendButton, pclmSendButton;
    private Button createJobButton;
    private Button sendDocumentButton;
    private EditText editText;

    private IppClientTransport transport;
    private String ippPath;
    private URI uri;

    private int mJobId = 0;


    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, "msg.what = "+msg.what);

            switch (msg.what){
                case 0:
                    textView.setText((String)msg.obj);
                    break;
                case 1:
                    textView.setText((String)msg.obj);
                    break;
                case 2:
                    textView.setText((String)msg.obj);
                    break;
                default:
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getResources().getConfiguration().locale.getCountry();

        textView = findViewById(R.id.message_received);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
        editText = findViewById(R.id.ipp_path);
        button = findViewById(R.id.button);
        button.setOnClickListener(this);
        sendFileButton = findViewById(R.id.send_file);
        sendFileButton.setOnClickListener(this);
        getJobButton = findViewById(R.id.get_job_status);
        getJobButton.setOnClickListener(this);
        pclmTestButton = findViewById(R.id.PCLmTest);
        pclmTestButton.setOnClickListener(this);
        pwgSendButton = findViewById(R.id.send_pwg);
        pwgSendButton.setOnClickListener(this);
        pclmSendButton = findViewById(R.id.send_pclm);
        pclmSendButton.setOnClickListener(this);
        createJobButton = findViewById(R.id.create_job);
        createJobButton.setOnClickListener(this);
        sendDocumentButton = findViewById(R.id.send_document);
        sendDocumentButton.setOnClickListener(MainActivity.this);

        initPrinter("ipp://10.0.0.100:631/ipp/print");
//        initPrinter("ipp://192.168.8.100:631/ipp/print");
    }

    private void initPrinter(String ippPath){
        uri = URI.create(ippPath);

        transport = new HttpIppClientTransport();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button:
                getPrinterAttributes();
                break;
            case R.id.send_file:
//                printJpg(Environment.getExternalStorageDirectory().getPath() + File.separator + "bitmap0.jpeg");
                printJpg("/sdcard/DCIM/Camera/magazine-unlock-01-2.3.875-_3414b9e834084686a6a5d4dacbfa7e97.jpg");
                break;
            case R.id.get_job_status:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "开始查询任务");
                        try {
                            getjobAttributes();
                        } catch (Exception e) {
                            Log.e(TAG, "查询任务异常 e = " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case R.id.PCLmTest:
                try {
                    RenderUtils.renderPDF(Environment.getExternalStorageDirectory().getPath() + "/Popo.pdf",
                            Environment.getExternalStorageDirectory().getPath() + "/Popo.PCLm");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.send_pwg:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            printPwg(Environment.getExternalStorageDirectory().getPath() + File.separator + "notice.pwg");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case R.id.send_pclm:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            printPclm(Environment.getExternalStorageDirectory().getPath() + File.separator + "Popo.PCLm");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case R.id.create_job:
                createJob();
                break;
            case R.id.send_document:
                sendDocument(Environment.getExternalStorageDirectory().getPath() + File.separator + "Popo.PCLm");
                break;
            default:
                break;
        }
    }





    public void getPrinterAttributes(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    IppPacket printRequest = new IppPacket(Operation.getPrinterAttributes, 123,
                            groupOf(operationAttributes,
                                    attributesCharset.of("utf-8"),
                                    attributesNaturalLanguage.of("en"),
                                    printerUri.of(uri),
                                    requestingUserName.of("jprint")));

                    Log.i(TAG, "Sending " + printRequest.prettyPrint(1200, "  "));
                    IppPacketData request = new IppPacketData(printRequest);
                    IppPacketData response = transport.sendData(uri, request);
//        Log.i(TAG, "Received: " + response.getPacket().prettyPrint(10000, "  "));

                    Message message = new Message();
                    message.what = 0;
                    message.obj = "Received: " + response.getPacket().prettyPrint(1000, "  ");
                    mHandler.sendMessage(message);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();


//        List<AttributeGroup> list = response.getPacket().getAttributeGroups();
//        for(AttributeGroup attributes:list){
//            for(int i =0; i<attributes.size(); i++){
//                Log.i(TAG, "Name = "+attributes.get(i).getName()+" Value = "+attributes.get(i).getValue());
//            }
//        }
    }

    public void getjobAttributes() throws IOException {
        Log.w(TAG, "开始查询 job 状态 id = "+mJobId);
        IppPacket printRequest = new IppPacket(Operation.getJobAttributes, 123,
                groupOf(operationAttributes,
                        attributesCharset.of("utf-8"),
                        attributesNaturalLanguage.of("en"),
                        printerUri.of(uri),
                        jobId.of(mJobId),
                        requestingUserName.of("jprint")));

        Log.i(TAG, "Sending " + printRequest.prettyPrint(1200, "  "));
        IppPacketData request = new IppPacketData(printRequest);
        IppPacketData response = transport.sendData(uri, request);
        Log.i(TAG, "Received: " + response.getPacket().prettyPrint(100, "  "));

        Message message = new Message();
        message.what = 2;
        message.obj = "Received: " + response.getPacket().prettyPrint(100, "  ");
        mHandler.sendMessage(message);
    }

    private void printJpg(String filePath){
        new Thread(new Runnable() {
            @Override
            public void run() {
                File inputFile;
                if(!TextUtils.isEmpty(filePath)){
                    inputFile = new File(filePath);
                    if(!inputFile.exists()){
                        Log.e(TAG, "文件不存在");
                        return;
                    }
                }else{
                    Log.e(TAG, "文件路径错误");
                    return;
                }

                IppPacket printRequest = new IppPacket(Operation.printJob, 123,
                        groupOf(operationAttributes,
                                attributesCharset.of("utf-8"),
                                attributesNaturalLanguage.of("en"),
                                printerUri.of(uri),
//                        copies.of(2),
                                requestingUserName.of("jprint"),
                                documentFormat.of("application/octet-stream")));

                Log.i(TAG, "Sending " + printRequest.prettyPrint(1200, "  "));
                IppPacketData response = null;
                try {
                    IppPacketData request = new IppPacketData(printRequest, new FileInputStream(inputFile));
                    response = transport.sendData(uri, request);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
                Log.w(TAG, "Received: " + response.getPacket().prettyPrint(100, "  "));
//        Log.e(TAG, "value = " + response.getPacket().getAttributeGroups().get(1).get("job-id").getValue().toString());
//        mJobId = Integer.valueOf(response.getPacket().getAttributeGroups().get(1).get("job-id").getValue().toString());

                Message message = new Message();
                message.what = 1;
                message.obj = "Received: " + response.getPacket().prettyPrint(100, "  ");
                mHandler.sendMessage(message);
            }
        }).start();
    }

    private void printPwg(String filePath) throws IOException{
        File inputFile;
        if(!TextUtils.isEmpty(filePath)){
            inputFile = new File(filePath);
            if(!inputFile.exists()){
                Log.e(TAG, "文件不存在");
                return;
            }
        }else{
            Log.e(TAG, "文件路径错误");
            return;
        }

        IppPacket printRequest = new IppPacket(Operation.printJob, 123,
                groupOf(operationAttributes,
                        attributesCharset.of("utf-8"),
                        attributesNaturalLanguage.of("en"),
                        printerUri.of(uri),
                        requestingUserName.of("jprint"),
//                        printColorMode.of("monochrome"),
//                        pclmSourceResolution.of(new Resolution(600, 600, ResolutionUnit.dotsPerInch)),
//                        documentFormat.of("application/octet-stream")));
//                        documentFormat.of("application/PCLm")));
                        documentFormat.of("image/pwg-raster")));

        Log.i(TAG, "Sending " + printRequest.prettyPrint(1200, "  "));
        IppPacketData request = new IppPacketData(printRequest, new FileInputStream(inputFile));
        IppPacketData response = transport.sendData(uri, request);
        Log.w(TAG, "Received: " + response.getPacket().prettyPrint(100, "  "));
//        Log.e(TAG, "value = " + response.getPacket().getAttributeGroups().get(1).get("job-id").getValue().toString());
        mJobId = Integer.valueOf(response.getPacket().getAttributeGroups().get(1).get("job-id").getValue().toString());

        Message message = new Message();
        message.what = 1;
        message.obj = "Received: " + response.getPacket().prettyPrint(100, "  ");
        mHandler.sendMessage(message);
    }

    private void printPclm(String filePath) throws IOException{
        File inputFile;
        if(!TextUtils.isEmpty(filePath)){
            inputFile = new File(filePath);
            if(!inputFile.exists()){
                Log.e(TAG, "文件不存在");
                return;
            }
        }else{
            Log.e(TAG, "文件路径错误");
            return;
        }

        IppPacket printRequest = new IppPacket(Operation.printJob, 123,
                groupOf(operationAttributes,
                        attributesCharset.of("utf-8"),
                        attributesNaturalLanguage.of("en"),
                        printerUri.of(uri),
                        requestingUserName.of("jprint"),
//                        printColorMode.of("monochrome"),
//                        pclmSourceResolution.of(new Resolution(600, 600, ResolutionUnit.dotsPerInch)),
//                        documentFormat.of("application/octet-stream")));
                        documentFormat.of("application/PCLm")));
//                        documentFormat.of("image/pwg-raster")));

        Log.i(TAG, "Sending " + printRequest.prettyPrint(1200, "  "));
        IppPacketData request = new IppPacketData(printRequest, new FileInputStream(inputFile));
        IppPacketData response = transport.sendData(uri, request);
        Log.w(TAG, "Received: " + response.getPacket().prettyPrint(100, "  "));
//        Log.e(TAG, "value = " + response.getPacket().getAttributeGroups().get(1).get("job-id").getValue().toString());
        mJobId = Integer.valueOf(response.getPacket().getAttributeGroups().get(1).get("job-id").getValue().toString());

        Message message = new Message();
        message.what = 1;
        message.obj = "Received: " + response.getPacket().prettyPrint(100, "  ");
        mHandler.sendMessage(message);
    }

    public void createJob(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.w(TAG, "开始createJob ");
                    IppPacket printRequest = new IppPacket(Operation.createJob, 123,
                            groupOf(operationAttributes,
                                    attributesCharset.of("utf-8"),
                                    attributesNaturalLanguage.of("en"),
                                    printerUri.of(uri),
                                    requestingUserName.of("createJob")));

                    Log.i(TAG, "Sending " + printRequest.prettyPrint(1200, "  "));
                    IppPacketData request = new IppPacketData(printRequest);
                    IppPacketData response = transport.sendData(uri, request);
                    Log.i(TAG, "Received: " + response.getPacket().prettyPrint(100, "  "));

                    mJobId = Integer.valueOf(response.getPacket().getAttributeGroups().get(1).get("job-id").getValue().toString());

                    Message message = new Message();
                    message.what = 2;
                    message.obj = "Received: " + response.getPacket().prettyPrint(100, "  ");
                    mHandler.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sendDocument(String filePath){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File inputFile;
                    if(!TextUtils.isEmpty(filePath)){
                        inputFile = new File(filePath);
                        if(!inputFile.exists()){
                            Log.e(TAG, "文件不存在");
                            return;
                        }
                    }else{
                        Log.e(TAG, "文件路径错误");
                        return;
                    }

                    IppPacket printRequest = new IppPacket(Operation.sendDocument, 123,
                            groupOf(operationAttributes,
                                    attributesCharset.of("utf-8"),
                                    attributesNaturalLanguage.of("en"),
                                    printerUri.of(uri),
                                    requestingUserName.of("createJob"),
                                    jobId.of(mJobId),
                                    lastDocument.of(true),
    //                        printColorMode.of("monochrome"),
    //                        pclmSourceResolution.of(new Resolution(600, 600, ResolutionUnit.dotsPerInch)),
    //                        documentFormat.of("application/octet-stream")));
                                    documentFormat.of("application/PCLm")));
//                        documentFormat.of("image/pwg-raster")));

                    Log.i(TAG, "Sending " + printRequest.prettyPrint(1200, "  "));
                    IppPacketData request = new IppPacketData(printRequest, new FileInputStream(inputFile));
                    IppPacketData response = transport.sendData(uri, request);
                    Log.w(TAG, "Received: " + response.getPacket().prettyPrint(100, "  "));
//        Log.e(TAG, "value = " + response.getPacket().getAttributeGroups().get(1).get("job-id").getValue().toString());
//        mJobId = Integer.valueOf(response.getPacket().getAttributeGroups().get(1).get("job-id").getValue().toString());

                    Message message = new Message();
                    message.what = 1;
                    message.obj = "Received: " + response.getPacket().prettyPrint(100, "  ");
                    mHandler.sendMessage(message);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
