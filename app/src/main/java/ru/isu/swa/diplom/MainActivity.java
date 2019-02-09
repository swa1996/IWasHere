package ru.isu.swa.diplom;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import ru.isu.swa.diplom.controller.Function;
import ru.isu.swa.diplom.controller.GPSLocation;
import ru.isu.swa.diplom.controller.IWHApi;
import ru.isu.swa.diplom.controller.ServiceGenerator;
import ru.isu.swa.diplom.model.Coordinate;
import ru.isu.swa.diplom.model.Mark;
import ru.isu.swa.diplom.controller.MarksAdapter;
import ru.isu.swa.diplom.view.AuthActivity;
import ru.isu.swa.diplom.view.FriendActivity;
import ru.isu.swa.diplom.view.GroupListActivity;
import ru.isu.swa.diplom.view.MessageActivity;
import ru.isu.swa.diplom.view.NewMarkActivity;
import ru.isu.swa.diplom.view.ProfileActivity;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Главный экран приложения
//Просмотр меток в заданной точке
public class MainActivity extends AppCompatActivity {
    private SharedPreferences mSettings;
    private SharedPreferences mKeys;
    private Drawer result;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Coordinate coord;
    private Double accuracy;
    private GPSLocation location;
    private Menu menu;
    private AccountHeader headerResult;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //проверяем наличие разрешений на геолокацию, если есть, то всё ok
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            grantPermiossion();
        }
        //получаем USER_ID, иначе перекидываем на AuthActivity
        mSettings = getSharedPreferences("USER", Context.MODE_PRIVATE);
        mKeys = getSharedPreferences("KEYS", Context.MODE_PRIVATE);
        if (!mSettings.contains("USER_ID")) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
        } else {
            //главная страница приложения(должен быть список всех меток в данной точке)
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
                                        onBackPressed();
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

            //боковое меню закончилось
            //Инициализация объекта GPSLocation, который работает с GPS
            location = GPSLocation.GPSLocationHolder.getGPSLocation(this);
            //загружаем RecyclerView
            recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            //Получаем компоненту SwipeRefreshLayout для управления поведением при обновлении списка
            mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    // Обновляем данные по сети
                    refreshCoords();
                }
            });
            refreshCoords();
              }

    }

    @Override
    //результат запроса разрешений
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 42 && grantResults.length == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Для работы приложения необходимо разрешение на определение местоположения", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //запрос разрешений
    public void grantPermiossion() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                42);
    }

    //боковое меню
    @Override
    protected void onSaveInstanceState(Bundle outState) {
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
                if(coord!=null) {
                    Intent intent1 = new Intent(this, NewMarkActivity.class);
                    startActivity(intent1);
                }else{
                    Toast.makeText(this, "Ваше местоположение не определено. Попробуйте позже", Toast.LENGTH_LONG).show();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
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
        menu.add(1, 1, 1, "Новая метка");
        return super.onCreateOptionsMenu(menu);
    }

    private void refresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);
        if (coord != null && accuracy!=null) {
            getSupportActionBar().setSubtitle(coord.getLatitude()+" "+coord.getLongitude()+" ("+accuracy+" м.)");
            Call<List<Mark>> call = apiService.getMarks(mSettings.getString("USER_COOKIE",""),mSettings.getInt("USER_ID",0),coord.getLatitude(),coord.getLongitude(), accuracy);
            //Обрабатываем ответ от сервера на запрос
            call.enqueue(new Callback<List<Mark>>() {
                @Override
                public void onResponse(Call<List<Mark>> call, Response<List<Mark>> response) {
                    int statusCode = response.code();
                    switch(statusCode) {
                        case 200:
                        final List<Mark> marks = response.body();
                        final List<Mark> removeMarks = new ArrayList<Mark>();
                                for(Mark mark: marks){
                                    if(mark.isEncrypted() && !(mKeys.contains("KEY_"+mark.getId()))) removeMarks.add(mark);
                                }
                                marks.removeAll(removeMarks);
                                        recyclerView.setAdapter(new MarksAdapter(marks, R.layout.list_marks, getApplicationContext()));

                        break;
                        case 401:
                            Toast.makeText(getApplicationContext(),"Сессия устарела",Toast.LENGTH_LONG).show();
                            Function.cleanCookie(mSettings);
                            startActivity(new Intent(getApplicationContext(),AuthActivity.class));
                            break;
                        case 404:
                            Toast.makeText(getApplicationContext(),"Пользователь не найден",Toast.LENGTH_LONG).show();
                            break;
                        case 500:
                            Toast.makeText(getApplicationContext(),"Ошибка сервера",Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(getApplicationContext(),"Неизвестная ошибка", Toast.LENGTH_LONG).show();

                    }
                }

                @Override
                public void onFailure(Call<List<Mark>> call, Throwable t) {
                    Toast.makeText(getApplicationContext(), "Ошибка: " + t.getMessage(),Toast.LENGTH_LONG).show();

                }
            });
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }
@Override
    public void onResume(){
        super.onResume();
        refreshCoords();
}
public void refreshCoords(){
    mSwipeRefreshLayout.setRefreshing(true);
    new Thread(new Runnable() {
        @Override
        public void run() {
            do {
                coord = GPSLocation.getCoords();
                accuracy = GPSLocation.getAccuracy();
                if(coord!=null && accuracy!=null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refresh();
                            mSwipeRefreshLayout.setRefreshing(false);

                        }
                    });
                }
            }
            while (coord == null || accuracy==null);

        }}).start();
}
}