package ru.isu.swa.diplom.view;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.controller.Function;
import ru.isu.swa.diplom.controller.IWHApi;
import ru.isu.swa.diplom.controller.ServiceGenerator;
import ru.isu.swa.diplom.model.User;
import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Регистрация в сервисе
public class RegActivity extends AppCompatActivity implements View.OnClickListener {
    TextView email;
    TextView phone;
    TextView firstName;
    TextView lastName;
    Button button;
    private static IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setSubtitle("Регистрация");
        email = (TextView) findViewById(R.id.email);
        phone = (TextView) findViewById(R.id.phoneNumber);
        firstName = (TextView) findViewById(R.id.firstName);
        lastName = (TextView) findViewById(R.id.lastName);
        button = (Button) findViewById(R.id.button);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Внимание!")
                .setMessage("Приложение находится в тестовом режиме. Все возникающие в результате использования данного приложения проблемы(сбои, ошибки, проблемы с устройством, ядерный апокалипсис) пользователь принимает на свой страх и риск. Владельцы сервиса не несут ответственности за публикуемую пользователями информацию.")
                .setCancelable(false)
                .setNegativeButton("Принимаю",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
        button.setOnClickListener(this);

    }

    @Override
    public void onClick(final View view) {
        switch(view.getId()){
            case R.id.button:
                //проверка на заполнение
                Set<Integer> validated = Function.validate(email, firstName, lastName, phone);
                if(validated.isEmpty()){
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        grantPermission();
                    }else{
                        register();
                    }
                }else{
                    String s = "Заполните поля:";
                    for(Integer num: validated){
                        switch(num){
                            case R.id.email:
                                s+=" Email,";
                                break;
                            case R.id.phoneNumber:
                                s+=" Номер телефона,";
                                break;
                            case R.id.firstName:
                                s+=" Имя,";
                                break;
                            case R.id.lastName:
                                s+=" Фамилия,";
                                break;
                        }

                    }
                    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
    @Override
    //результат запроса разрешений
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 42 && grantResults.length == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            register();
            }else{
                Toast.makeText(this, "Регистрация отменена", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //запрос разрешений
    public void grantPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Разрешения")
                .setMessage("Для завершения регистрации вам необходимо предоставить приложению разрешения на доступ к данным о местоположении. Указанные данные используется только для обеспечения работы сервиса и нигде не сохраняются.")
                .setCancelable(true)
                .setPositiveButton("Продолжить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(RegActivity.this,
                                new String[]{
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                },
                                42);
                    }
                })
                .setNegativeButton("Отмена",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                Toast.makeText(getApplicationContext(), "Регистрация отменена", Toast.LENGTH_LONG).show();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();

    }
public void register(){
    //создаем объект User
    User user = new User();
    user.setEmail(email.getText().toString());
    user.setPhoneNumber(phone.getText().toString());
    user.setFirstName(firstName.getText().toString());
    user.setLastName(lastName.getText().toString());

    Call<User> call = apiService.register(user);
    call.enqueue(new Callback<User>() {
        @Override
        public void onResponse(Call<User> call, Response<User> response) {
            int statusCode = response.code();
            switch (statusCode) {
                case 400:
                    Toast.makeText(getApplicationContext(), "Пользователь с данным Email уже зарегистрирован", Toast.LENGTH_LONG).show();
                    break;
                case 500:
                    Toast.makeText(getApplicationContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                    break;
                case 200:
                    final User user = response.body();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(RegActivity.this);
                            builder.setTitle("Регистрация")
                                    .setMessage(user.getFirstName()+" "+user.getLastName()+", вы успешно зарегистрированы в сервисе IWasHere. Для того, чтобы начать пользоваться всеми возможностями приложения, вам необходимо войти в систему. Пароль для входа будет выслан на ваш email.")
                                    .setCancelable(false)
                                    .setNegativeButton("Войти",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                    Intent intent = new Intent(getApplicationContext(), AuthActivity.class);
                                                    intent.putExtra("email", user.getEmail());
                                                    startActivity(intent);
                                                }
                                            });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    });
                    break;
                default:
                    Toast.makeText(getApplicationContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(Call<User> call, Throwable t) {
            String mes = t.getMessage();
            Toast.makeText(getApplicationContext(), "Ошибка: " + mes, Toast.LENGTH_LONG).show();

        }
    });
}
}
