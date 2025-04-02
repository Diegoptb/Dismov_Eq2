package com.project.eq2.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.eq2.R;
import com.project.eq2.utilities.Constants;
import com.project.eq2.utilities.PreferenceManager;

import java.util.Locale;

public class SignInActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private MaterialButton buttonSignIn;
    private ProgressBar signInProgress;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Inicializa preferenceManager primero
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Aplica el idioma guardado
        applySavedLanguage();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        signInProgress = findViewById(R.id.progressBarSignIn);

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }

        findViewById(R.id.textSignUp).setOnClickListener(view ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));

        inputEmail    = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        buttonSignIn  = findViewById(R.id.buttonSignIn);

        buttonSignIn.setOnClickListener(view -> {
            if (inputEmail.getText().toString().trim().isEmpty()) {
                Toast.makeText(SignInActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(inputEmail.getText().toString()).matches()) {
                Toast.makeText(SignInActivity.this, "Enter valid email", Toast.LENGTH_SHORT).show();
            } else if (inputPassword.getText().toString().trim().isEmpty()) {
                Toast.makeText(SignInActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
            } else {
                signIn();
            }
        });

        // Botón para cambiar idioma
        Button btnChangeLanguage = findViewById(R.id.btnChangeLanguage);
        btnChangeLanguage.setOnClickListener(view -> switchLanguage());
    }

    // Metodo para aplicar el idioma guardado
    private void applySavedLanguage() {
        String language = preferenceManager.getString("APP_LANGUAGE");
        if (language == null || language.isEmpty()) {
            return; // Si no hay idioma guardado, usa el predeterminado
        }

        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }


    private void switchLanguage() {
        // Obtener idioma actual del sistema
        String currentLanguage = getResources().getConfiguration().locale.getLanguage();

        // Alternar idioma: si está en inglés, cambia a español; si está en español, cambia a inglés
        String newLanguage = currentLanguage.equals("en") ? "es" : "en";

        // Guardar la preferencia del idioma
        preferenceManager.putString("APP_LANGUAGE", newLanguage);

        // Aplicar el nuevo idioma
        Locale newLocale = new Locale(newLanguage);
        Locale.setDefault(newLocale);

        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(newLocale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        // Reiniciar la actividad para aplicar los cambios
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setLocale(String lang) {
        Locale newLocale = new Locale(lang);
        Locale.setDefault(newLocale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(newLocale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "es"); // Español por defecto
        setLocale(language);
    }

    private void signIn() {
        buttonSignIn.setVisibility(View.INVISIBLE);
        signInProgress.setVisibility(View.VISIBLE);

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, inputEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD, inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_FIRST_NAME, documentSnapshot.getString(Constants.KEY_FIRST_NAME));
                        preferenceManager.putString(Constants.KEY_LAST_NAME, documentSnapshot.getString(Constants.KEY_LAST_NAME));
                        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        signInProgress.setVisibility(View.INVISIBLE);
                        buttonSignIn.setVisibility(View.VISIBLE);
                        Toast.makeText(SignInActivity.this, "Unable to sign in", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
