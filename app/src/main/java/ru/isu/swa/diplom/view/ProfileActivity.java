package ru.isu.swa.diplom.view;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.Arrays;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.isu.swa.diplom.MainActivity;
import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.controller.Function;
import ru.isu.swa.diplom.controller.IWHApi;
import ru.isu.swa.diplom.controller.ServiceGenerator;
import ru.isu.swa.diplom.model.User;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Профиль пользователя
public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {
    private User user;
    private Integer USER_ID;
    private String USER_COOKIE;
    private static IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);;
    private SharedPreferences mSettings;
    private Drawer result;
    private TextView editFirstName;
    private TextView editLastName;
    private TextView editPhone;
    private TextView editEmail;
    private TextView text;
    private Button button;
    private LinearLayout exitAll;
    private AccountHeader headerResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        editFirstName = (TextView) findViewById(R.id.editFirstName);
        editLastName = (TextView) findViewById(R.id.editLastName);
        editEmail = (TextView) findViewById(R.id.editEmail);
        editPhone = (TextView) findViewById(R.id.editPhone);
        text = (TextView) findViewById(R.id.textProfile);
        button = (Button) findViewById(R.id.buttonProfile);
        exitAll = (LinearLayout) findViewById(R.id.layoutExitAll);
        mSettings = getSharedPreferences("USER", Context.MODE_PRIVATE);
        editEmail.setEnabled(false);
        text.setText("Здесь вы можете изменить настройки своего профиля\nОбращаем внимание, что адрес Email изменению не подлежит");
        new Thread(new Runnable() {
            @Override
            public void run() {
                USER_ID = mSettings.getInt("USER_ID", 0);
                USER_COOKIE = mSettings.getString("USER_COOKIE", "");
                final Integer user_id = getIntent().getIntExtra("user_id", 0);
                user = Function.getUser(USER_COOKIE, USER_ID, USER_ID);
                if (user != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            editEmail.setText(user.getEmail());
                            editFirstName.setText(user.getFirstName());
                            editLastName.setText(user.getLastName());
                            editPhone.setText(user.getPhoneNumber());
                        }
                    });
            }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Ошибка загрузки данных", Toast.LENGTH_LONG).show();
                        }
                    });   }
            }
        }).start();

        //боковое меню
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setSubtitle("Профиль");
        final IProfile profile = new ProfileDrawerItem().withName(mSettings.getString("USER_FIRSTNAME","")+" "+mSettings.getString("USER_LASTNAME","")).withEmail(mSettings.getString("USER_EMAIL",""));
        headerResult = new AccountHeaderBuilder().withHeaderBackground(R.color.colorPrimaryDark)
                .withActivity(this)
                .withCompactStyle(true)
                .addProfiles(
                        profile
                )
                .withSavedInstance(savedInstanceState)
                .build();
        result = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Метки").withIdentifier(1),
                        new PrimaryDrawerItem().withName("Профиль").withIdentifier(2),
                        new PrimaryDrawerItem().withName("Друзья").withIdentifier(3),
                        new PrimaryDrawerItem().withName("Группы").withIdentifier(4),
                        new PrimaryDrawerItem().withName("Сообщения").withIdentifier(5),
                        new SecondaryDrawerItem().withName("Выход").withIdentifier(6)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
//обработка выбранного меню
                            switch((int)drawerItem.getIdentifier()){
                                case 1:
                                    //метки
                                    startActivity(new Intent(getApplicationContext(),MainActivity.class));
                                    break;
                                case 2:
                                    //Переход на профиль
                                    startActivity(new Intent(getApplicationContext(),ProfileActivity.class));
                                    break;
                                case 3:
                                    //Друзья
                                    startActivity(new Intent(getApplicationContext(),FriendActivity.class));
                                    break;
                                case 4:
                                    //Группы
                                    startActivity(new Intent(getApplicationContext(), GroupListActivity.class));
                                    break;
                                case 5:
                                    //Сообщения
                                    startActivity(new Intent(getApplicationContext(), MessageActivity.class));
                                    break;
                                case 6:
                                    //Выход
                                    Function.exit(view.getContext(), mSettings);
                                    break;
                            }
                        }
                        return false;

                    }}).withSavedInstance(savedInstanceState).build();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);
//боковое меню кончилось
        button.setOnClickListener(this);
        exitAll.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.buttonProfile:
                //нажатие на кнопку
                Set<Integer> validated = Function.validate(editEmail, editFirstName, editLastName, editPhone);
                if(validated.isEmpty()){
                    user.setFirstName(editFirstName.getText().toString());
                    user.setLastName(editLastName.getText().toString());
                    user.setPhoneNumber(editPhone.getText().toString());
                    Call<User> call = apiService.changeUser(USER_COOKIE,user.getId(), user.getPhoneNumber(), user.getFirstName(), user.getLastName());
                    call.enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            int statusCode = response.code();
                            switch(statusCode) {
                                case 200:
                                    User user = response.body();
                                    Toast.makeText(getApplicationContext(), "Данные успешно изменены", Toast.LENGTH_LONG).show();
                                    final SharedPreferences.Editor editor = mSettings.edit();
                                    editor.putString("USER_EMAIL", user.getEmail());
                                    editor.putString("USER_FIRSTNAME", user.getFirstName());
                                    editor.putString("USER_LASTNAME", user.getLastName());
                                    editor.commit();
                                    IProfile profile = headerResult.getActiveProfile();
                                    profile.withName(user.getFirstName()+" "+user.getLastName());
                                    headerResult.setProfiles(Arrays.asList(profile));
                                    break;
                                case 401:
                                    Toast.makeText(getApplicationContext(),"Сессия устарела",Toast.LENGTH_LONG).show();
                                    Function.cleanCookie(mSettings);
                                    startActivity(new Intent(getApplicationContext(),AuthActivity.class));
                                    break;
                                case 404:
                                    Toast.makeText(getApplicationContext(),"Пользователь не найден",Toast.LENGTH_LONG).show();
                                    break;
                                case 400:
                                    Toast.makeText(getApplicationContext(),"Неправильный запрос",Toast.LENGTH_LONG).show();
                                    break;
                                case 500:
                                    Toast.makeText(getApplicationContext(),"Ошибка сервера",Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    Toast.makeText(getApplicationContext(),"Неизвестная ошибка", Toast.LENGTH_LONG).show();

                            }
                        }

                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), t.getMessage(),Toast.LENGTH_LONG).show();

                        }
                    });
                }else{
                    String s = "Заполните поля:";
                    for(Integer num: validated){
                        switch(num){
                            case R.id.editEmail:
                                s+=" Email,";
                                break;
                            case R.id.editPhone:
                                s+=" Номер телефона,";
                                break;
                            case R.id.editFirstName:
                                s+=" Имя,";
                                break;
                            case R.id.editLastName:
                                s+=" Фамилия,";
                                break;
                        }

                    }
                    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.layoutExitAll:
                //нажатие на ссылку "Выйти везде"
                AlertDialog.Builder ad = new AlertDialog.Builder(this);
                ad.setTitle("Выйти везде");  // заголовок
                ad.setMessage("Будут закрыты все активные сессии, кроме текущей.\nВы действительно хотите продолжить?"); // сообщение
                ad.setPositiveButton("Выйти везде", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        Call<Void> call = apiService.exitAll(USER_COOKIE, USER_ID, mSettings.getInt("USER_PUBLICKEY_ID",0));
                        //Обрабатываем ответ от сервера на запрос
                        call.enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                //код ответа сервера (200 - ОК), в данном случае далее не используется
                                int statusCode = response.code();
                                switch (statusCode) {
                                    case 200:
                                        Toast.makeText(getApplicationContext(), "Выход успешно завершен", Toast.LENGTH_LONG).show();
                                        break;
                                    case 401:
                                        Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                        intent1.putExtra("email", mSettings.getString("USER_EMAIL",""));
                                        Function.cleanCookie(mSettings);
                                        startActivity(intent1);
                                    case 404:
                                        Toast.makeText(getApplicationContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                                        break;
                                    case 400:
                                        Toast.makeText(getApplicationContext(), "Неверный запрос", Toast.LENGTH_LONG).show();
                                        break;
                                    case 500:
                                        Toast.makeText(getApplicationContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(getApplicationContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                String mes = t.getMessage();
                            }
                        });
                    }
                });
                ad.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                    }
                });
                AlertDialog alertDialog = ad.create();

                alertDialog.show();
                break;
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState = result.saveInstanceState(outState);
        outState = headerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onBackPressed() {
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
        }
        return super.onOptionsItemSelected(item);
    }
}
