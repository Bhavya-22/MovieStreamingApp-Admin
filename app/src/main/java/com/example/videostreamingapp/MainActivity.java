package com.example.videostreamingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.videostreamingapp.Model.VideoUploadDetails;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    Uri videoUri;
    TextView text_video_selected;
    String videoCategory;
    String videoTitle;
    String currentUid;
    StorageReference mstorageRef;
    StorageTask mUploadsTask;
    DatabaseReference referenceVideos;
    EditText video_description;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text_video_selected=findViewById(R.id.textvideoselected);
        video_description=findViewById(R.id.movies_description);
        referenceVideos= FirebaseDatabase.getInstance().getReference().child("Videos");
        mstorageRef=FirebaseStorage.getInstance().getReference().child("Videos");

        Spinner spinner=findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);

        List<String> categories =new ArrayList<>();
        categories.add("Action");
        categories.add("Adventure");
        categories.add("Sports");
        categories.add("Romantic");
        categories.add("Comedy");

        ArrayAdapter<String> dataAdapter= new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        videoCategory= parent.getItemAtPosition(position).toString();
        Toast.makeText(this,"Selected: " +videoCategory,Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    public void openVideoFiles(View view)
        {
            //To open select video option from upload video btn
            Intent videoIntent = new Intent(Intent.ACTION_GET_CONTENT);
            videoIntent.setType("video/*");
            startActivityForResult(videoIntent,101);

        }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==101 && resultCode==RESULT_OK && data.getData() !=null)
        {
            videoUri=data.getData();

            String path= null;
            Cursor cursor;
            int column_indexed_data;
            String[] projection= {MediaStore.MediaColumns.DATA,MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media._ID,MediaStore.Video.Thumbnails.DATA};
            final String orderBy=MediaStore.Video.Media.DEFAULT_SORT_ORDER;
            cursor= MainActivity.this.getContentResolver().query(videoUri,projection,null,
                    null,orderBy);
            column_indexed_data=cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext())
            {
                path=cursor.getString(column_indexed_data);
                videoTitle= FilenameUtils.getBaseName(path);
            }
            text_video_selected.setText(videoTitle);
        }
    }
    public void uploadFileToFirebase(View v)
    { //To upload foles when onclick upload is pressed
        if(text_video_selected.equals("no video selected"))
        {
            Toast.makeText(this,"Please select a video to upload",Toast.LENGTH_SHORT).show();
        }
        else
            {
                if(mUploadsTask !=null && mUploadsTask.isInProgress())
                {
                    Toast.makeText(this,"Video uploads is all ready in progress ...",Toast.LENGTH_SHORT).show();
                }
                else
                    {
                        uploadFiles();
                    }
            }
    }

    private void uploadFiles() {
        if(videoUri !=null)
        {
            final ProgressDialog progressDialog=new ProgressDialog(this);
            progressDialog.setMessage("video uploading...");
            progressDialog.show();
            final StorageReference storageReference=mstorageRef.child(videoTitle);
            mUploadsTask=storageReference.putFile(videoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                  storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                      @Override
                      public void onSuccess(Uri uri) {
                          String video_url=uri.toString();
                          VideoUploadDetails videoUploadDetails =new VideoUploadDetails("","",
                                  "",
                                  video_url,videoTitle,video_description.getText().toString(),videoCategory);

                          String uploadsid=referenceVideos.push().getKey();
                          referenceVideos.child(uploadsid).setValue(videoUploadDetails);
                          currentUid=uploadsid;
                          progressDialog.dismiss();
                          if(currentUid.equals(uploadsid))
                          {
                              startThumbnailsActivity();
                          }
                      }
                  });
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {

                    double progress=(100.0 * taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                    progressDialog.setMessage("Uploaded " + ((int)progress) +"%...");
                }
            });
        }
        else
            {
                Toast.makeText(this,"No video selected to upload",Toast.LENGTH_SHORT).show();
            }
    }
    public void startThumbnailsActivity()
    {
        Intent in =new Intent(MainActivity.this,UploadThumbnailActivity.class);
        in.putExtra("currentuid",currentUid);
        in.putExtra("thumbnailsName",videoTitle);
        startActivity(in);
        Toast.makeText(this,"Video uploaded successfully upload video thumbnail",Toast.LENGTH_SHORT).show();
    }
}
