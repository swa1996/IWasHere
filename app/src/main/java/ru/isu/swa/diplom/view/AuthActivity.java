package ru.isu.swa.diplom.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.isu.swa.diplom.MainActivity;
import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.controller.Function;
import ru.isu.swa.diplom.controller.IWHApi;
import ru.isu.swa.diplom.controller.ServiceGenerator;
import ru.isu.swa.diplom.model.Key;
import ru.isu.swa.diplom.model.User;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Аутентификация
public class AuthActivity extends AppCompatActivity implements View.OnClickListener {

    TextView email;
    TextView password;
    Button button;
    TextView textView;
    LinearLayout register;
    private static IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);
    private SharedPreferences mSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        mSettings = getSharedPreferences("USER", Context.MODE_PRIVATE);
        email = (TextView) findViewById(R.id.email);
        password = (TextView) findViewById(R.id.password);
        button = (Button) findViewById(R.id.button);
        textView = (TextView) findViewById(R.id.textView);
        register = (LinearLayout) findViewById(R.id.register);
        password.setEnabled(false);
        button.setOnClickListener(this);
        register.setOnClickListener(this);
        Intent intent = getIntent();
        email.setText(intent.getStringExtra("email"));
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setSubtitle("Вход в приложение");
    }

    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.register:
                Intent intent = new Intent(this, RegActivity.class);
                startActivity(intent);
                break;
            case R.id.button:
            if (email.getText().toString().isEmpty()) {
                Toast.makeText(this, "Email не заполнен", Toast.LENGTH_LONG).show();
            } else {
                //пароль пустой, нужно запросить
                if (password.getText().toString().isEmpty()) {
                    Call<Void> call = apiService.getPassword(email.getText().toString());
                    //Обрабатываем ответ от сервера на запрос
                    call.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            int statusCode = response.code();
                            switch (statusCode) {
                                case 404:
                                    Toast.makeText(v.getContext(), "Пользователь с таким email не найден", Toast.LENGTH_LONG).show();
                                    break;
                                case 500:
                                    Toast.makeText(v.getContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                                    break;
                                case 200:
                                    runOnUiThread(new Runnable() {
                                                      @Override
                                                      public void run() {
                                                          textView.setText("На ваш email " + email.getText().toString() + " был выслан одноразовый пароль для входа. Пожалуйста, введите его в поле ниже\n Если пароль не пришел, запросите его еще раз, нажав кнопку \"Войти\"");
                                                          password.setEnabled(true);
                                                          button.setText("Войти");
                                                      }
                                                  }
                                    );
                                    break;
                                default:
                                    Toast.makeText(v.getContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            String mes = t.getMessage();
                            Toast.makeText(v.getContext(), "Текст: " + mes, Toast.LENGTH_LONG).show();

                        }
                    });

                } else {
                    //email и пароль введены, пробуем войти
                    Call<User> call = apiService.auth(email.getText().toString(), password.getText().toString());
                    //Обрабатываем ответ от сервера на запрос
                    call.enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            int statusCode = response.code();
                            switch (statusCode) {
                                case 404:
                                    Toast.makeText(v.getContext(), "Пользователя или пароля не существует", Toast.LENGTH_LONG).show();
                                    password.setText("");
                                    button.setText("Получить пароль");
                                    break;
                                case 408:
                                    Toast.makeText(v.getContext(), "Истек срок действия пароля. Запросите новый пароль", Toast.LENGTH_LONG).show();
                                    password.setText("");
                                    password.setEnabled(false);
                                    button.setText("Получить пароль");
                                    break;
                                case 429:
                                    Toast.makeText(v.getContext(), "Вы неверно ввели пароль 3 раза подряд. Запросите новый пароль", Toast.LENGTH_LONG).show();
                                    password.setText("");
                                    password.setEnabled(false);
                                    button.setText("Получить пароль");
                                    break;
                                case 403:
                                    Toast.makeText(v.getContext(), "Неверный пароль", Toast.LENGTH_LONG).show();
                                    break;
                                case 500:
                                    Toast.makeText(v.getContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                                    break;
                                case 200:
                                    //Успех! Запоминаем юзера
                                    User user = (User) response.body();
                                    final SharedPreferences.Editor editor = mSettings.edit();
                                    editor.putInt("USER_ID", user.getId());
                                    editor.putString("USER_EMAIL", user.getEmail());
                                    editor.putString("USER_FIRSTNAME", user.getFirstName());
                                    editor.putString("USER_LASTNAME", user.getLastName());
                                    editor.putString("USER_COOKIE",response.headers().get("Set-Cookie"));
                                    //Создаем ключи шифрования
                                    final KeyPairGenerator keyGen;
                                    try {
                                        keyGen = KeyPairGenerator.getInstance("RSA");
                                        keyGen.initialize(2048, new SecureRandom());
                                        final KeyPair keys = keyGen.generateKeyPair();
                                        editor.putString("USER_PRIVATEKEY", Function.bytesToHexString(keys.getPrivate().getEncoded()));
                                        final Key key = new Key(Function.bytesToHexString(keys.getPublic().getEncoded()));
                                        editor.putString("USER_PUBLICKEY", key.getKey());
                                        Call call1 = apiService.addKey(mSettings.getString("USER_COOKIE",""),user.getId(), key);
                                        //Обрабатываем ответ от сервера на запрос
                                        call1.enqueue(new Callback<Key>() {
                                            @Override
                                            public void onResponse(Call<Key> call, Response<Key> response) {
                                                int statusCode = response.code();
                                                switch (statusCode) {
                                                    case 400:
                                                        Toast.makeText(v.getContext(), "Данные не заполнены", Toast.LENGTH_LONG).show();
                                                        break;
                                                    case 404:
                                                        Toast.makeText(v.getContext(), "Пользователь с таким email не найден", Toast.LENGTH_LONG).show();
                                                        break;
                                                    case 500:
                                                        Toast.makeText(v.getContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                                                        break;
                                                    case 200:
                                                        Key key = response.body();
                                                        editor.putInt("USER_PUBLICKEY_ID", key.getId());
                                                        editor.commit();
                                                        break;
                                                    case 401:
                                                        Toast.makeText(v.getContext(), "Ошибка авторизации", Toast.LENGTH_LONG).show();
                                                    default:
                                                        Toast.makeText(v.getContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call call, Throwable t) {

                                            }
                                        });
                                    } catch (NoSuchAlgorithmException e) {
                                        e.printStackTrace();
                                    }

                                    editor.apply();
                                    Intent intent = new Intent(AuthActivity.super.getApplicationContext(), MainActivity.class);
                                    startActivity(intent);
                                    break;
                                default:
                                    Toast.makeText(v.getContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            String mes = t.getMessage();
                            Toast.makeText(v.getContext(), mes, Toast.LENGTH_LONG).show();
                        }
                    });

                }

            }
            break;
        }
    }


}
