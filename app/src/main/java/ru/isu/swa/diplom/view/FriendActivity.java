package ru.isu.swa.diplom.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.isu.swa.diplom.MainActivity;
import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.controller.Function;
import ru.isu.swa.diplom.controller.IWHApi;
import ru.isu.swa.diplom.controller.ServiceGenerator;
import ru.isu.swa.diplom.controller.UserAdapter;
import ru.isu.swa.diplom.model.User;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Список друзей
public class FriendActivity extends AppCompatActivity {
    private SharedPreferences mSettings;
    private Drawer result;
    private RecyclerView recyclerView;
    private AccountHeader headerResult;
    private Integer USER_ID;
    private String USER_COOKIE;
    private User user;
    private TextView searchText;
    private Button searchButton;
    IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);
        recyclerView = (RecyclerView) findViewById(R.id.friendRView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSettings = getSharedPreferences("USER", Context.MODE_PRIVATE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                USER_ID = mSettings.getInt("USER_ID",0);
                USER_COOKIE = mSettings.getString("USER_COOKIE","");
                user = Function.getUser(USER_COOKIE, USER_ID, USER_ID);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(user!=null) {
                            loadFriends();
                        }else{
                            Toast.makeText(getApplicationContext(), "Ошибка загрузки данных", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        }}).start();
        //боковое меню
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setSubtitle("Список друзей");
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
                .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
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
                                    onBackPressed();
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
        searchText = (TextView) findViewById(R.id.searchText);
        searchButton = (Button) findViewById(R.id.buttonSearch);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchUser();
            }
        });
//боковое меню кончилось
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
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = result.saveInstanceState(outState);
        outState = headerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }
public void loadFriends(){
    recyclerView.setAdapter(new UserAdapter(new ArrayList<User>(user.getFriends()), R.layout.list_users, this));
}
public void searchUser(){
        String searchString = searchText.getText().toString();
    Call<List<User>> call = apiService.searchUser(USER_COOKIE, USER_ID, searchString);
    call.enqueue(new Callback<List<User>>() {
        @Override
        public void onResponse(Call<List<User>> call, Response<List<User>> response) {
            int statusCode = response.code();
            switch (statusCode) {
                case 200:
                    Toast.makeText(getApplicationContext(), "Запрос выполнен", Toast.LENGTH_SHORT).show();
                    getSupportActionBar().setSubtitle("Поиск пользователей");
                    List<User> users = response.body();
                    recyclerView.setAdapter(new UserAdapter(users, R.layout.list_users, getApplicationContext()));
                    break;
                case 401:
                    Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                    Function.cleanCookie(mSettings);
                    startActivity(new Intent(getApplicationContext(), AuthActivity.class));
                    break;
                case 404:
                    Toast.makeText(getApplicationContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                    break;
                case 500:
                    Toast.makeText(getApplicationContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(getApplicationContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();

            }
        }

        @Override
        public void onFailure(Call<List<User>> call, Throwable t) {
            Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    });
}
}
