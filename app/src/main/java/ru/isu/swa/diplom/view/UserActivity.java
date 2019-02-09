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
import java.util.HashSet;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.isu.swa.diplom.MainActivity;
import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.controller.Function;
import ru.isu.swa.diplom.controller.IWHApi;
import ru.isu.swa.diplom.controller.ServiceGenerator;
import ru.isu.swa.diplom.controller.UserAdapter;
import ru.isu.swa.diplom.model.Message;
import ru.isu.swa.diplom.model.User;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Просмотр профиля пользователя
public class UserActivity extends AppCompatActivity {
    private User myUser;
    private User user;
    private Menu menu;
    private Integer USER_ID;
    private String USER_COOKIE;
    private static IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);;
    private SharedPreferences mSettings;
    private Drawer result;
    private TextView firstName;
    private TextView lastName;
    private TextView phoneNumber;
    private TextView email;
    private RecyclerView rview;
    private AccountHeader headerResult;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        firstName = (TextView) findViewById(R.id.firstName);
        lastName = (TextView) findViewById(R.id.lastName);
        email = (TextView) findViewById(R.id.email);
        phoneNumber = (TextView) findViewById(R.id.phoneNumber);
        rview = (RecyclerView) findViewById(R.id.rView);
        rview.setLayoutManager(new LinearLayoutManager(this));
        mSettings = getSharedPreferences("USER", Context.MODE_PRIVATE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                USER_ID = mSettings.getInt("USER_ID",0);
                USER_COOKIE = mSettings.getString("USER_COOKIE","");
                final Integer user_id = getIntent().getIntExtra("user_id",0);
                    myUser = Function.getUser(USER_COOKIE, USER_ID, USER_ID);
                    if(user_id!=0)user = Function.getUser(USER_COOKIE, USER_ID, user_id);
                    if(myUser!=null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refresh();
                            }
                        });
                    }else{
                        Toast.makeText(getApplicationContext(), "Ошибка загрузки данных", Toast.LENGTH_LONG).show();
                    }
                }
        }).start();
        //боковое меню
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
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


    }
    public void refresh(){

            MenuItem deleteFriend = menu.findItem(2);
            MenuItem addFriend = menu.findItem(1);
            MenuItem sendMessage = menu.findItem(3);
        if(user==null){
            getSupportActionBar().setTitle("Аноним");
            getSupportActionBar().setSubtitle("IWasHere");
            firstName.setText("Мы - Анонимус");
            lastName.setText("Имя нам - Легион");
            email.setText("Анонимус не прощает");
            phoneNumber.setText("Правил не существует!");
            deleteFriend.setVisible(false);
            addFriend.setVisible(false);
            sendMessage.setVisible(false);
            ((TextView)findViewById(R.id.textFriends)).setVisibility(View.INVISIBLE);
        }else{
            getSupportActionBar().setTitle(user.getFirstName()+" "+user.getLastName());
            getSupportActionBar().setSubtitle("Пользователь IWasHere");
            firstName.setText("Имя: "+user.getFirstName());
            lastName.setText("Фамилия: "+ user.getLastName());
            if(user.getFriends().contains(myUser) || user.equals(myUser)){
                phoneNumber.setText("Телефон: "+user.getPhoneNumber());
                email.setText("Email: "+user.getEmail());
            }else{
                email.setText("Email: [скрыт]");
                phoneNumber.setText("Телефон: [скрыт]");
                sendMessage.setVisible(false);
            }
            if(myUser.getFriends().contains(user))addFriend.setVisible(false);
            else deleteFriend.setVisible(false);
            if(user.equals(myUser)){
                addFriend.setVisible(false);
                deleteFriend.setVisible(false);
                sendMessage.setVisible(false);
            }

            rview.setAdapter(new UserAdapter(new ArrayList(user.getFriends()), R.layout.list_users, this));
        }

    }
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        menu.add(1, 2, 1, "Удалить из друзей");
        menu.add(1, 1, 1, "Добавить в друзья");
        menu.add(1, 3, 1, "Отправить сообщение");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case 1:
                //добавление в друзья (если еще не добавлен)
                addFriend();
                break;
            case 2:
                //удаление из друзей (если уже добавлен)
                deleteFriend();
                break;
            case 3:
                //отправка сообщения (только для взаимных друзей)
                sendMessage();
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

public void addFriend(){
    Call<User> call = apiService.addFriend(USER_COOKIE, USER_ID, getIntent().getIntExtra("user_id",0));
    //Обрабатываем ответ от сервера на запрос
    call.enqueue(new Callback<User>() {
        @Override
        public void onResponse(Call<User> call, Response<User> response) {
            int statusCode = response.code();
            switch(statusCode) {
                case 200:
                    Toast.makeText(getApplicationContext(), user.getFirstName()+" "+user.getLastName()+" добавлен в ваш список друзей", Toast.LENGTH_LONG).show();
                    MenuItem deleteFriend = menu.findItem(2);
                    MenuItem addFriend = menu.findItem(1);
                    deleteFriend.setVisible(true);
                    addFriend.setVisible(false);
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
                    Toast.makeText(getApplicationContext(),"Неправильный запрос. Возможно, пользователь был добавлен в друзья ранее",Toast.LENGTH_LONG).show();
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
}

public void deleteFriend(){
    Call<User> call = apiService.deleteFriend(USER_COOKIE, USER_ID, getIntent().getIntExtra("user_id",0));
    call.enqueue(new Callback<User>() {
        @Override
        public void onResponse(Call<User> call, Response<User> response) {
            int statusCode = response.code();
            switch(statusCode) {
                case 200:
                    Toast.makeText(getApplicationContext(), user.getFirstName()+" "+user.getLastName()+" удален из вашего списка друзей", Toast.LENGTH_LONG).show();
                    MenuItem deleteFriend = menu.findItem(2);
                    MenuItem addFriend = menu.findItem(1);
                    deleteFriend.setVisible(false);
                    addFriend.setVisible(true);
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
                    Toast.makeText(getApplicationContext(),"Неправильный запрос. Возможно, пользователь не входит в ваш список друзей",Toast.LENGTH_LONG).show();
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
}
public void sendMessage(){
    LayoutInflater li = LayoutInflater.from(this);
    View promptsView = li.inflate(R.layout.layout_sendmessage, null);

    AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(this);

    mDialogBuilder.setView(promptsView);

    final EditText userInput = (EditText) promptsView.findViewById(R.id.newMessage);

    mDialogBuilder
            .setCancelable(true)
            .setPositiveButton("Отправить",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            Message mes = new Message();
                            mes.setAuthor(myUser);
                            mes.setMessage(userInput.getText().toString());
                            Set<User> receivers = new HashSet();
                            receivers.add(user);
                            mes.setReceivers(receivers);
                            Call<Message> call = apiService.sendMessage(USER_COOKIE, USER_ID, mes);
                            call.enqueue(new Callback<Message>() {
                                @Override
                                public void onResponse(Call<Message> call, Response<Message> response) {
                                    int statusCode = response.code();
                                    switch (statusCode) {
                                        case 200:
                                            Toast.makeText(getApplicationContext(), "Сообщение успешно отравлено", Toast.LENGTH_LONG).show();
                                            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                                            intent.putExtra("sub_id", user.getId());
                                            intent.putExtra("sub", "user");
                                            startActivity(intent);
                                          break;
                                        case 401:
                                            Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                            Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                            Function.cleanCookie(mSettings);
                                            intent1.putExtra("email", mSettings.getString("USER_EMAIL",""));
                                            startActivity(intent1);
                                            break;
                                        case 403:
                                            Toast.makeText(getApplicationContext(), "Вы не можете отправить сообщения данному пользователю", Toast.LENGTH_LONG).show();
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
                                public void onFailure(Call<Message> call, Throwable t) {
                                    String mes = t.getMessage();
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

    AlertDialog alertDialog = mDialogBuilder.create();

    alertDialog.show();

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
}
