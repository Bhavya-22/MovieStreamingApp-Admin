package com.example.videostreamingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

public class UploadThumbnailActivity extends AppCompatActivity {

    Uri videothumburi;
    String thumbnail_url;
    ImageView thumbnail_image;
    StorageReference mStoragerefthumbnails;
    DatabaseReference referenceVideos;
    TextView textSelected;
    RadioButton radioButtonlatest,radioButtonNotype,radioButtonpopular,radioButtonSlide;
    StorageTask mStorageTask;
    DatabaseReference  updatedataref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_thumbnail);
        textSelected=findViewById(R.id.textNoThumbnailSelected);
        thumbnail_image=findViewById(R.id.imageView);
        radioButtonlatest=findViewById(R.id.radioLatestMov);
        radioButtonpopular=findViewById(R.id.radioBestPopularMov);
        radioButtonNotype=findViewById(R.id.radioNotype);
        radioButtonSlide=findViewById(R.id.radioSlideMov);
        mStoragerefthumbnails = FirebaseStorage.getInstance().getReference().child("VideoThumbnails");
        referenceVideos= FirebaseDatabase.getInstance().getReference().child("Videos");
        String currentUid= getIntent().getExtras().getString("currentuid");
        updatedataref =FirebaseDatabase.getInstance().getReference("Videos").child(currentUid);

        radioButtonNotype.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //String latestMovies=radioButtonNotype.getText().toString();
                updatedataref.child("video_type").setValue("");
                updatedataref.child("video_slide").setValue("");
                Toast.makeText(UploadThumbnailActivity.this,"Selected: No type" ,Toast.LENGTH_SHORT).show();
            }
        });

        radioButtonlatest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String latestMovies=radioButtonlatest.getText().toString();
                updatedataref.child("video_type").setValue(latestMovies);
                updatedataref.child("video_slide").setValue("");
                Toast.makeText(UploadThumbnailActivity.this,"Selected: " +latestMovies,Toast.LENGTH_SHORT).show();
            }
        });
        radioButtonpopular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String popularMovies=radioButtonpopular.getText().toString();
                updatedataref.child("video_type").setValue(popularMovies);
                updatedataref.child("video_slide").setValue("");
                Toast.makeText(UploadThumbnailActivity.this,"Selected: " +popularMovies,Toast.LENGTH_SHORT).show();
            }
        });
        radioButtonSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String slideMovies=radioButtonSlide.getText().toString();
                updatedataref.child("video_type").setValue("");
                updatedataref.child("video_slide").setValue(slideMovies);
                Toast.makeText(UploadThumbnailActivity.this,"Selected: " +slideMovies,Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void showImageChooser(View view)
    {
        //On click upload thumbnail need to open gallery
        Intent in= new Intent(Intent.ACTION_GET_CONTENT);
        in.setType("image/*");
        startActivityForResult(in,102);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    if(requestCode==102 && resultCode==RESULT_OK && data.getData() !=null)
    {
        videothumburi=data.getData();
        try{
            String thumbname =getFileName(videothumburi);
            textSelected.setText(thumbname);
            Bitmap bitmap= MediaStore.Images.Media.getBitmap(getContentResolver(),videothumburi);
            thumbnail_image.setImageBitmap(bitmap);
        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    }

    private String getFileName(Uri uri)
    {
        String result=null;
        if(uri.getScheme().equals("content"))
        {
            Cursor cursor=getContentResolver().query(uri,null,null,null,null);
            try
            {
               if(cursor !=null && cursor.moveToFirst())
               {
                   result=cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
               }
            } finally {
                cursor.close();
            }
        }
        if(result==null)
        {
            result=uri.getPath();
            int cut=result.lastIndexOf("/");
            if(cut != -1)
            {
                result=result.substring(cut +1);
            }
        }
        return result;
    }
    private void uploadFiles()
    {
        if(videothumburi!=null)
        {
            final ProgressDialog progressDialog=new ProgressDialog(this);
            progressDialog.setMessage("Wait uploading thumbnail...");
            progressDialog.show();
            String video_title=getIntent().getExtras().getString("thumbnailsName");

            final StorageReference sRef=mStoragerefthumbnails.child(video_title+"." +getFileExtension(videothumburi));

            sRef.putFile(videothumburi).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    sRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            thumbnail_url=uri.toString();
                            updatedataref.child("video_thumb").setValue(thumbnail_url);
                            progressDialog.dismiss();
                            Toast.makeText(UploadThumbnailActivity.this,"Files Uploaded",Toast.LENGTH_SHORT).show();

                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(UploadThumbnailActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double progress=(100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                progressDialog.setMessage("Uploaded " +(int)progress +"%...");
                }
            });
        }
    }
    public void uploadFileToFirebase(View view)
    {   //For on click upload in thumbnail activity
        if(textSelected.getText().toString().equals("No Thumbnail Selected"))
    {
        Toast.makeText(this,"First Select an image",Toast.LENGTH_SHORT).show();
    }else{
            if(mStorageTask !=null && mStorageTask.isInProgress())
            {
                Toast.makeText(this,"Upload files all ready in progress",Toast.LENGTH_SHORT).show();
            }
            else {
                uploadFiles();
            }
        }

    }

    public String getFileExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mimeTypeMap=MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cr.getType(uri));
    }
}
