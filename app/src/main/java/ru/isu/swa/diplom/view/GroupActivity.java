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

import java.io.IOException;
import java.util.ArrayList;
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
import ru.isu.swa.diplom.controller.UserAdapter;
import ru.isu.swa.diplom.model.Group;
import ru.isu.swa.diplom.model.Message;
import ru.isu.swa.diplom.model.User;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Просмотр группы
public class GroupActivity extends AppCompatActivity implements View.OnLongClickListener {
    private SharedPreferences mSettings;
    private Drawer result;
    private RecyclerView recyclerView;
    private AccountHeader headerResult;
    private Integer USER_ID;
    private String USER_COOKIE;
    private User user;
    private Group group;
    private LinearLayout showAdmin;
    private TextView showAdminText;
    private LinearLayout showChat;
    private TextView groupInfo;
    private boolean isShowedAdmin = false;
    Menu menu;
    IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        recyclerView = (RecyclerView) findViewById(R.id.groupMemberRView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        showAdmin = (LinearLayout) findViewById(R.id.showAdminLayout);
        showAdminText = (TextView) findViewById(R.id.showAdmin);
        showChat = (LinearLayout) findViewById(R.id.showChat);
        groupInfo = (TextView) findViewById(R.id.groupInfo);
        showAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isShowedAdmin){
                    showAdminText.setText("Показать администраторов");
                    isShowedAdmin = false;
                    showMembers();
                }else{
                    showAdminText.setText("Показать пользователей");
                    isShowedAdmin = true;
                    showAdmins();
                }
            }
        });
        showChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                intent.putExtra("sub", "group");
                intent.putExtra("sub_id", group.getId());
                startActivity(intent);
            }
        });
        mSettings = getSharedPreferences("USER", Context.MODE_PRIVATE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Integer GROUP_ID = getIntent().getIntExtra("group_id",0);
                USER_ID = mSettings.getInt("USER_ID",0);
                USER_COOKIE = mSettings.getString("USER_COOKIE","");
                user = Function.getUser(USER_COOKIE, USER_ID, USER_ID);
                if(user!=null) {
                    Call<Group> call = apiService.getGroup(USER_COOKIE, USER_ID, GROUP_ID);
                    call.enqueue(new Callback<Group>() {
                        @Override
                        public void onResponse(Call<Group> call, Response<Group> response) {
                            int statusCode = response.code();
                            switch (statusCode) {
                                case 200:
                                    group = response.body();
                                    groupInfo.setText(group.getMembers().size() + " участников");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (menu != null) {
                                                if (group.getAdministrators().contains(user)) {
                                                    menu.add(1, 1, 1, "Добавить участника");
                                                    menu.add(1, 2, 1, "Изменить группу");
                                                } else {
                                                    menu.add(1, 3, 1, "Выйти из группы");
                                                }
                                            }
                                        }
                                    });
                                    showMembers();
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
                        public void onFailure(Call<Group> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }else{
                    Toast.makeText(getApplicationContext(), "Ошибка загрузки данных", Toast.LENGTH_LONG).show();
                }

            }}).start();
        //боковое меню
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
                Call<List<User>> call = apiService.getFriends(USER_COOKIE,USER_ID);
                call.enqueue(new Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                        int statusCode = response.code();
                        switch (statusCode) {
                            case 200:
                                final List<User> friends = response.body();
                                friends.removeAll(group.getMembers());
                             //   final List<User> users = new ArrayList(friends);
                                AlertDialog.Builder builder = new AlertDialog.Builder(GroupActivity.this);
                                builder.setTitle("Добавить в группу")
                                        .setCancelable(true)
                                        .setItems(getNames(friends), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                final User addedUser = friends.get(which);
                                                Call<Group> call = apiService.addMember(USER_COOKIE, USER_ID, group.getId(), addedUser.getId());
                                                call.enqueue(new Callback<Group>() {
                                                    @Override
                                                    public void onResponse(Call<Group> call, Response<Group> response) {
                                                        int statusCode = response.code();
                                                        switch (statusCode) {
                                                            case 200:
                                                                group = response.body();
                                                                Toast.makeText(getApplicationContext(), "Пользователь добавлен", Toast.LENGTH_LONG).show();
                                                                break;
                                                            case 401:
                                                                Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                                                Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                                                Function.cleanCookie(mSettings);
                                                                intent1.putExtra("email", mSettings.getString("USER_EMAIL", ""));
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
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        dialog.cancel();
                                                    }
                                                });
                                AlertDialog alert = builder.create();
                                alert.show();
                                break;
                            case 401:
                                Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                Function.cleanCookie(mSettings);
                                intent1.putExtra("email", mSettings.getString("USER_EMAIL", ""));
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
                    public void onFailure(Call<List<User>> call, Throwable t) {
                        String mes = t.getMessage();
                        Toast.makeText(getApplicationContext(), mes, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case 2:
                //изменение группы
                LayoutInflater li = LayoutInflater.from(this);
                View promptsView = li.inflate(R.layout.layout_newgroup, null);

                AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(this);

                mDialogBuilder.setView(promptsView);

                final EditText userInput = (EditText) promptsView.findViewById(R.id.newGroup);
                userInput.setText(group.getName());

                mDialogBuilder
                        .setCancelable(true)
                        .setPositiveButton("Изменить",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        group.setName(userInput.getText().toString());

                                        Call<Group> call = apiService.chengeGroup(USER_COOKIE, USER_ID, group);
                                        call.enqueue(new Callback<Group>() {
                                            @Override
                                            public void onResponse(Call<Group> call, Response<Group> response) {
                                                int statusCode = response.code();
                                                switch (statusCode) {
                                                    case 200:
                                                        group = response.body();
                                                        getSupportActionBar().setSubtitle(group.getName());
                                                        Toast.makeText(getApplicationContext(), "Группа изменена", Toast.LENGTH_LONG).show();
                                                        break;
                                                    case 401:
                                                        Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                                        Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                                        Function.cleanCookie(mSettings);
                                                        intent1.putExtra("email", mSettings.getString("USER_EMAIL",""));
                                                        startActivity(intent1);
                                                        break;
                                                    case 403:
                                                        Toast.makeText(getApplicationContext(), "Вы не можете менять эту группу", Toast.LENGTH_LONG).show();
                                                        break;
                                                    case 404:
                                                        Toast.makeText(getApplicationContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                                                        break;
                                                    case 400:
                                                        Toast.makeText(getApplicationContext(), "Неверный запрос", Toast.LENGTH_LONG).show();
                                                        break;
                                                    case 500:
                                                        Toast.makeText(getApplicationContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                                                        try {
                                                            Toast.makeText(getApplicationContext(), response.errorBody().string(), Toast.LENGTH_LONG).show();
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
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
            case 3:
                //выход из группы(для участник, добровольный)
                if(!group.getAdministrators().contains(user)) {
                    AlertDialog.Builder ad = new AlertDialog.Builder(this);
                    ad.setTitle("Выйти из группы");  // заголовок
                    ad.setMessage("Вы действительно хотите выйти из группы"); // сообщение
                    ad.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int arg1) {
                            Call<Group> call = apiService.deleteMember(USER_COOKIE, USER_ID, group.getId(),user.getId());
                            //Обрабатываем ответ от сервера на запрос
                            call.enqueue(new Callback<Group>() {
                                @Override
                                public void onResponse(Call<Group> call, Response<Group> response) {
                                    //код ответа сервера (200 - ОК), в данном случае далее не используется
                                    int statusCode = response.code();
                                    switch (statusCode) {
                                        case 200:
                                            Toast.makeText(getApplicationContext(), "Вы вышли из группы", Toast.LENGTH_LONG).show();
                                            onBackPressed();
                                            break;
                                        case 401:
                                            Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                            Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                            intent1.putExtra("email", mSettings.getString("USER_EMAIL", ""));
                                            Function.cleanCookie(mSettings);
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
                    });
                    ad.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int arg1) {
                        }
                    });
                    //Создаем AlertDialog:
                    AlertDialog alertDialog1 = ad.create();

                    //и отображаем его:
                    alertDialog1.show();
                }
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
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
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

public void showMembers(){
    getSupportActionBar().setSubtitle(group.getName());
    recyclerView.setAdapter(new UserAdapter(new ArrayList<User>(group.getMembers()), R.layout.list_users, this));

}
public void showAdmins(){
    recyclerView.setAdapter(new UserAdapter(new ArrayList<User>(group.getAdministrators()), R.layout.list_users, this));
}

    @Override
    public boolean onLongClick(View v) {
        if(group.getAdministrators().contains(user)) {
            final User clickedUser = (User) v.getTag();
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setTitle(clickedUser.toString());  // заголовок
            ad.setMessage("Выберите действие"); // сообщение
            if (group.getAdministrators().contains(clickedUser)) {
                ad.setPositiveButton("Удалить администратора", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        Call<Group> call = apiService.deleteAdministrator(USER_COOKIE, USER_ID, group.getId(), clickedUser.getId());
                        call.enqueue(new Callback<Group>() {
                            @Override
                            public void onResponse(Call<Group> call, Response<Group> response) {
                                int statusCode = response.code();
                                switch (statusCode) {
                                    case 200:
                                        group = response.body();
                                        Toast.makeText(getApplicationContext(), "Администратор успешно удален", Toast.LENGTH_LONG).show();
                                        break;
                                    case 401:
                                        Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                        Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                        intent1.putExtra("email", mSettings.getString("USER_EMAIL", ""));
                                        Function.cleanCookie(mSettings);
                                        startActivity(intent1);
                                        break;
                                    case 403:
                                        Toast.makeText(getApplicationContext(), "Вы не можете удалить администратора в этой группе", Toast.LENGTH_LONG).show();
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
                });
            }
            if (group.getMembers().contains(clickedUser) && !group.getAdministrators().contains(clickedUser)) {
                ad.setPositiveButton("Удалить из группы", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Call<Group> call = apiService.deleteMember(USER_COOKIE, USER_ID, group.getId(), clickedUser.getId());
                        //Обрабатываем ответ от сервера на запрос
                        call.enqueue(new Callback<Group>() {
                            @Override
                            public void onResponse(Call<Group> call, Response<Group> response) {
                                //код ответа сервера (200 - ОК), в данном случае далее не используется
                                int statusCode = response.code();
                                switch (statusCode) {
                                    case 200:
                                        group = response.body();
                                        Toast.makeText(getApplicationContext(), "Пользователь удален из группы", Toast.LENGTH_LONG).show();
                                        break;
                                    case 401:
                                        Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                        Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                        intent1.putExtra("email", mSettings.getString("USER_EMAIL", ""));
                                        Function.cleanCookie(mSettings);
                                        startActivity(intent1);
                                        break;
                                    case 403:
                                        Toast.makeText(getApplicationContext(), "Вы не можете удалить пользователя из группы", Toast.LENGTH_LONG).show();
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
                });
                ad.setNeutralButton("Добавить администратора", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Call<Group> call = apiService.addAdministrator(USER_COOKIE, USER_ID, group.getId(), clickedUser.getId());
                        //Обрабатываем ответ от сервера на запрос
                        call.enqueue(new Callback<Group>() {
                            @Override
                            public void onResponse(Call<Group> call, Response<Group> response) {
                                //код ответа сервера (200 - ОК), в данном случае далее не используется
                                int statusCode = response.code();
                                switch (statusCode) {
                                    case 200:
                                        group = response.body();
                                        Toast.makeText(getApplicationContext(), "Пользователь добавлен в список администратора", Toast.LENGTH_LONG).show();
                                        break;
                                    case 401:
                                        Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                        Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                        intent1.putExtra("email", mSettings.getString("USER_EMAIL", ""));
                                        Function.cleanCookie(mSettings);
                                        startActivity(intent1);
                                        break;
                                    case 403:
                                        Toast.makeText(getApplicationContext(), "Вы не можете управлять этой группой", Toast.LENGTH_LONG).show();
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
                });
            }
            ad.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                }
            });
            AlertDialog alertDialog = ad.create();

            alertDialog.show();
            return true;
        }else{
            return false;
        }

    }
    public String[] getNames(List<? extends Object> objects){
        String[] result = new String[objects.size()];
        for(int i=0;i<result.length;i++){
            result[i] = objects.get(i).toString();
        }
        return result;
    }
}
