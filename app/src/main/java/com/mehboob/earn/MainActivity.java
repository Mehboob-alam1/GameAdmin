package com.mehboob.earn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.Manifest;
import android.provider.MediaStore;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mehboob.earn.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
private ActivityMainBinding binding;
    private static final int REQUEST_GALLERY_IMAGE = 100;
    private static final int REQUEST_CAMERA_IMAGE = 200;
    private static final int PERMISSION_CAMERA = 201;
    private static final int PERMISSION_EXTERNAL_STORAGE = 202;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    Uri selectedImageUri;
    private ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        dialog= new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage("Please wait....");
storageReference= FirebaseStorage.getInstance().getReference("Banners");
databaseReference= FirebaseDatabase.getInstance().getReference("Banners");



        binding.btnUploadSlider.setOnClickListener(view -> {
         if (selectedImageUri ==null)
             Toast.makeText(this, "Select any image", Toast.LENGTH_SHORT).show();
         else if (binding.etEditText.getText().toString().isEmpty())
             Toast.makeText(this, "provide a link", Toast.LENGTH_SHORT).show();
         else
             uploaddata(binding.etEditText.getText().toString(),selectedImageUri);
        });


        binding.btnPickImage.setOnClickListener(view -> {
            checkPermissionsAndPickImage();
        });
    }

    private void uploaddata(String link, Uri selectedImageUri) {
        dialog.show();

        String fileName = "images/" + UUID.randomUUID().toString(); // Set a unique filename for the image
        StorageReference imageRef = storageReference.child(fileName);

// Upload the file to Firebase Storage
        UploadTask uploadTask = imageRef.putFile(selectedImageUri);

// Register a listener to track the upload progress and handle the completion
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // File upload success
                // Handle the completion, e.g., get the download URL
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri downloadUri) {
                        // File upload is complete, and you have the download URL
                        String imageUrl = downloadUri.toString();
                        // Use the imageUrl as needed (e.g., save it to a database)
                        uploadToDataBase(imageUrl,link);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
dialog.dismiss();
                        Toast.makeText(MainActivity.this, ""+exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        // Handle any errors that occurred while retrieving the download URL
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors that occurred during the upload
                dialog.dismiss();
                Toast.makeText(MainActivity.this, ""+exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void uploadToDataBase(String imageUrl, String link) {
        String pushId=UUID.randomUUID().toString();
        Slider slider = new Slider(imageUrl,link,pushId);

        databaseReference.child(pushId)
                .setValue(slider)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        dialog.dismiss();
                        Toast.makeText(this, "Slider added successfully", Toast.LENGTH_SHORT).show();
                    }else{
                        dialog.dismiss();
                        Toast.makeText(this, "Something went wrong ", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(this, ""+e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void checkPermissionsAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_EXTERNAL_STORAGE);
        } else {
            showImagePickDialog();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissionsAndPickImage();
            }
        } else if (requestCode == PERMISSION_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImagePickDialog();
            }
        }
    }
    private void showImagePickDialog() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Option");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, REQUEST_CAMERA_IMAGE);
                } else if (options[item].equals("Choose from Gallery")) {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, REQUEST_GALLERY_IMAGE);
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY_IMAGE) {
              selectedImageUri = data.getData();

                binding.imageSlider.setImageURI(selectedImageUri);
                // Do something with the selected image URI
            } else if (requestCode == REQUEST_CAMERA_IMAGE) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap photo = (Bitmap) extras.get("data");
                    try {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        photo.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                        String path = MediaStore.Images.Media.insertImage(getContentResolver(), photo, "Title", null);
                        selectedImageUri = Uri.parse(path);

                        binding.imageSlider.setImageURI(selectedImageUri);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Do something with the captured image bitmap
                }
            }
        }
    }

}