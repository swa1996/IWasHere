package ru.isu.swa.diplom.view;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.isu.swa.diplom.MainActivity;
import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.controller.Function;
import ru.isu.swa.diplom.controller.GroupAdapter;
import ru.isu.swa.diplom.controller.IWHApi;
import ru.isu.swa.diplom.controller.ServiceGenerator;
import ru.isu.swa.diplom.model.Group;
import ru.isu.swa.diplom.model.User;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Список групп пользователя
public class GroupListActivity extends AppCompatActivity {
    private SharedPreferences mSettings;
    private Drawer result;
    private RecyclerView recyclerView;
    private AccountHeader headerResult;
    private Integer USER_ID;
    private String USER_COOKIE;
    private User user;
    private Menu menu;
    IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);
        recyclerView = (RecyclerView) findViewById(R.id.groupRView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSettings = getSharedPreferences("USER", Context.MODE_PRIVATE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                USER_ID = mSettings.getInt("USER_ID",0);
                USER_COOKIE = mSettings.getString("USER_COOKIE","");
                user = Function.getUser(USER_COOKIE, USER_ID, USER_ID);
                if(user!=null) {
                    Call<List<Group>> call = apiService.getGroups(USER_COOKIE, USER_ID);
                    call.enqueue(new Callback<List<Group>>() {
                        @Override
                        public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                            int statusCode = response.code();
                            switch (statusCode) {
                                case 200:
                                    final List<Group> groups = response.body();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            recyclerView.setAdapter(new GroupAdapter(groups, R.layout.list_groups, getApplicationContext()));
                                        }
                                    });
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
                        public void onFailure(Call<List<Group>> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Ошибка загрузки данных", Toast.LENGTH_LONG).show();
                        }
                    });
                 }

            }}).start();
        //боковое меню
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setSubtitle("Группы");
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
                                    startActivity(new Intent(getApplicationContext(),FriendActivity.class));
                                    break;
                                case 4:
                                    //Группы
                                    onBackPressed();
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
    }
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = result.saveInstanceState(outState);
        outState = headerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case 1:
                LayoutInflater li = LayoutInflater.from(this);
                View promptsView = li.inflate(R.layout.layout_newgroup, null);

                //Создаем AlertDialog
                AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(this);

                //Настраиваем prompt.xml для нашего AlertDialog:
                mDialogBuilder.setView(promptsView);

                //Настраиваем отображение поля для ввода текста в открытом диалоге:
                final EditText userInput = (EditText) promptsView.findViewById(R.id.newGroup);

                //Настраиваем сообщение в диалоговом окне:
                mDialogBuilder
                        .setCancelable(true)
                        .setPositiveButton("Создать",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        Group group = new Group();
                                        group.setName(userInput.getText().toString());
                                        Set<User> users = new HashSet();
                                        users.add(user);
                                        group.setAdministrators(users);
                                        group.setMembers(users);
                                        //Вводим текст и отображаем в строке ввода на основном экране:
                                        Call<Group> call = apiService.addGroup(USER_COOKIE, USER_ID, group);
                                        //Обрабатываем ответ от сервера на запрос
                                        call.enqueue(new Callback<Group>() {
                                            @Override
                                            public void onResponse(Call<Group> call, Response<Group> response) {
                                                //код ответа сервера (200 - ОК), в данном случае далее не используется
                                                int statusCode = response.code();
                                                switch (statusCode) {
                                                    case 200:
                                                        Group group = response.body();
                                                        Toast.makeText(getApplicationContext(), "Группа успешно создана", Toast.LENGTH_LONG).show();
                                                        Intent intent = new Intent(getApplicationContext(), GroupActivity.class);
                                                        intent.putExtra("group_id", group.getId());
                                                        startActivity(intent);
                                                        break;
                                                    case 401:
                                                        Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                                        Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                                        Function.cleanCookie(mSettings);
                                                        intent1.putExtra("email", mSettings.getString("USER_EMAIL",""));
                                                        startActivity(intent1);
                                                        break;
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
                                            public void onFailure(Call<Group> call, Throwable t) {
                                                String mes = t.getMessage();
                                                Toast.makeText(getApplicationContext(), mes, Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                    }
                                })
                        .setNegativeButton("Отмена",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                //Создаем AlertDialog:
                AlertDialog alertDialog = mDialogBuilder.create();

                //и отображаем его:
                alertDialog.show();
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
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

    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        menu.add(1, 1, 1, "Создать группу");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
    }




}
