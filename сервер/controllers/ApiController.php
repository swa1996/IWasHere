<?php

namespace app\controllers;

use yii\web\Controller;
use yii\data\Pagination;
use app\models\User;
use app\models\Password;
use app\models\Login;
use app\models\Key;
use app\models\Coordinate;
use app\models\Mark;
use app\models\Message;
use app\models\MessageKeys;
use app\models\Group;
use Yii;
use DateTime;
use DateInterval;
use yii\web\BadRequestHttpException;
use yii\web\NotFoundHttpException;
use yii\web\ForbiddenHttpException;
use yii\web\UnauthorizedHttpException;

class ApiController extends Controller {
    /*  ФУНКЦИИ API    */
    /* Обрабатывают запросы пользователя    */

    //Аутентификация: получение одноразового пароля
    public function actionAuth() {
        $request = Yii::$app->request;
        $email = $request->post('email');
        self::checkEmpty($email);
        $user = self::getUser(0, $email);
        Password::deleteAll(['user_id' => $user->id]);
        $pass = new Password;
        $pass->user_id = $user->id;
        $random = random_int(1000000, 9999999);
        Yii::$app->mailer->compose()
                ->setFrom('iwh@i-swa.ru')
                ->setTo($email)
                ->setSubject('Вход в приложение IWasHere')
                ->setTextBody("Ваш одноразовый пароль для входа в приложение IWasHere: $random")
                ->send();
        $date = new DateTime;
        $pass->date = $date->format('Y-m-d H:i');
        $pass->password = password_hash($random, PASSWORD_DEFAULT);
        $pass->save();
    }

    //Аутентификация: ввод одноразового пароля
    public function actionAuth2() {
        $request = Yii::$app->request;
        $email = $request->post('email');
        $password = $request->post('password');
        self::checkEmpty($email, $password);
        $user = self::getUser(0, $email);
        $pass = Password::find()->where(['user_id' => $user->id])->one();
        if (!isset($pass))
            throw new NotFoundHttpException($message = "Пользователь не запрашивал пароль", $code = 404);
        $date = new DateTime;
        $date2 = new DateTime($pass->date);
        if ($date->sub(new DateInterval("PT1H")) > $date2) {
            $pass->delete();
            Login::deleteAll(['user_id' => $user->id, 'result' => false]);
            throw new \yii\web\HttpException(408);
        }
        $login = new Login;
        $login->user = $user;
        $login->date = $date->format('Y-m-d H:i');
        $login_count = Login::find()->where(['user_id' => $user->id, 'result' => false])->count();
        if (password_verify($password, $pass->password)) {
            $login->result = true;
            $random = self::generateKey(32);
            $login->session_key = password_hash($random, PASSWORD_DEFAULT);
            $login->save();
            $cookies = Yii::$app->response->cookies;
            $cookies->remove('session_key');
            $id=$login->id;
            $cookie = new \yii\web\Cookie([
                'name' => 'session_key',
                'value' => "$id/$random",
            ]);
            $cookie->expire = time() + 10 * 365 * 24 * 60 * 60;
// добавление новой куки в HTTP-ответ
            $cookies->add($cookie);
            $pass->delete();
            Login::deleteAll(['user_id' => $user->id, 'result' => false]);
        } else {
            $login->result = false;
            if ($login_count >= 2) {
                $pass->delete();
                Login::deleteAll(['user_id' => $user->id, 'result' => false]);
                throw new \yii\web\HttpException(429);
            }
            $login->save();
            throw new ForbiddenHttpException($message = "Пароль неверен!", $code = 403);
        }
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::JSONUser($user);
    }

    //Регистрация нового пользователя
    public function actionReg() {
        $request = Yii::$app->request;
        $user = file_get_contents('php://input');
        self::checkEmpty($user);
        $user = json_decode($user);
        $email = $user->{'email'};
        $us = User::find()->where(['email' => $email])->one();
        if (isset($us))
            throw new BadRequestHttpException($message = "Пользователь с данным Email уже зарегистрирован", $code = 400);
        $newuser = new User;
        $newuser->email = $email;
        $newuser->phoneNumber = $user->{'phoneNumber'};
        $newuser->firstName = $user->{'firstName'};
        $newuser->lastName = $user->{'lastName'};

        $newuser->save();
     
        Yii::$app->mailer->compose()
                ->setFrom('iwh@i-swa.ru')
                ->setTo($email)
                ->setSubject('Регистрация в приложении IWasHere')
                ->setTextBody("$newuser->firstName $newuser->lastName, вы успешно зарегистрировались в приложении IWasHere! Для использования всех возможностей сервиса вам необходимо войти в приложение\n С уважением, команда IWasHere\n")
                ->send();
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::JSONUser($newuser);
    }

    //Изменение данных профиля пользователя
    public function actionChangeUser() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $phone = $request->post('phoneNumber');
        $firstName = $request->post('firstName');
        $lastName = $request->post('lastName');
        self::checkEmpty($user_id, $phone, $firstName, $lastName);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $userDB->phoneNumber = $phone;
        $userDB->firstName = $firstName;
        $userDB->lastName = $lastName;
        $userDB->save();
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::JSONUser($userDB);
    }

    //Добавление ключа пользователя
    public function actionAddKey() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        # Получить JSON как строку
        $key = file_get_contents('php://input');
        self::checkEmpty($user_id, $key);
        $key = json_decode($key);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $keyObj = new Key;
        $keyObj->pkey = $key->{'pkey'};
        $userDB->link('keys', $keyObj);
        $keyObj->save();

        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return $keyObj;
    }


    //Добавление друга
    public function actionAddFriend() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $friend_id = $request->post('friend_id');
        self::checkEmpty($user_id, $friend_id);
        $userDB = self::getUser($user_id);
        $friendDB = self::getUser($friend_id);
        self::checkCookie($userDB->id);
        if (in_array($friendDB, $userDB->friends))
            throw new BadRequestHttpException($message = "Пользователь уже добавлен", $code = 400);
        $userDB->link('friends', $friendDB);
        $mes = new Message();
        $mes->message = "Пользователь $userDB->firstName $userDB->lastName добавил Вас в свой список друзей.";
        $mes->link('author', $userDB);
        $mes->link('receivers', $friendDB);
        $date = new DateTime;
        $mes->date = $date->format('Y-m-d H:i');
        $mes->save();
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::JSONUser($userDB);
    }

    //Удаление друга
    public function actionDeleteFriend() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $friend_id = $request->post('friend_id');
        self::checkEmpty($user_id, $friend_id);
        $userDB = self::getUser($user_id);
        $friendDB = self::getUser($friend_id);
        self::checkCookie($userDB->id);
        $userDB->unlink('friends', $friendDB, true);
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::JSONUser($userDB);
    }

    //Получение инфы о юзере
    public function actionUserInfo() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $sub_id = $request->post('sub_id');
        self::checkEmpty($user_id, $sub_id);
        $userDB = self::getUser($user_id);
        $subDB = self::getUser($sub_id);
        self::checkCookie($userDB->id);
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::JSONUser($subDB);
    }
    
        //Получение метки
    public function actionGetMark() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $mark_id = $request->post('mark_id');
        self::checkEmpty($user_id, $mark_id);
        $userDB = self::getUser($user_id);
        $markDB = Mark::findOne($mark_id);
        self::checkCookie($userDB->id);
        if(isset($markDB)){
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::MarkToJSON($markDB);
        }
    }
    
       //Получение группы
    public function actionGetGroup() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $group_id = $request->post('group_id');
        self::checkEmpty($user_id, $group_id);
        $userDB = self::getUser($user_id);
        $groupDB = Group::findOne($group_id);
        self::checkCookie($userDB->id);
        if(isset($groupDB)){
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::GroupToJSON($groupDB);
        }
    }

    //Добавление новой метки
    public function actionAddMark() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $mark = json_decode(file_get_contents('php://input'));
        self::checkEmpty($user_id, $mark);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $coord = Coordinate::find()->where(['longitude' => $mark->{'coords'}->{'longitude'}, 'latitude' => $mark->{'coords'}->{'latitude'}])->one();
        if (!isset($coord)) {
            $coord = new Coordinate;
            $coord->latitude = $mark->{'coords'}->{'latitude'};
            $coord->longitude = $mark->{'coords'}->{'longitude'};
            $coord->save();
        }
        $markDB = new Mark;
        $markDB->message = $mark->{'message'};
        $markDB->isAnonymed = $mark->{'isAnonymed'};
        $markDB->isEncrypted = $mark->{'isEncrypted'};
        $date = new DateTime;
        $markDB->date = $date->format('Y-m-d H:i');
        $markDB->coords = $coord->id;
        $markDB->link('author', $userDB);
        $markDB->save();
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::MarkToJSON($markDB);
    }

    //Изменение метки
    public function actionChangeMark() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $mark = json_decode(file_get_contents('php://input'));
        self::checkEmpty($user_id, $mark);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);

        $markDB = Mark::find()->where(['id' => $mark->{'id'}])->one();
        if (!isset($markDB))
            throw new NotFoundHttpException($message = "Метка не найдена", $code = 404);
        if ($markDB->author != $userDB->id)
            throw new ForbiddenHttpException($message = "Изменить метку может только её автор!", $code = 403);
        $date = new DateTime;
        $date2 = new DateTime($markDB->date);
        if ($date->sub(new DateInterval("PT1H")) > $date2)
            throw new \yii\web\HttpException(408); //изменение метки доступно в течение часа после добавления
            
        $markDB->message = $mark->{'message'};
        $markDB->isAnonymed = $mark->{'isAnonymed'};
        $markDB->isEncrypted = $mark->{'isEncrypted'};
        $markDB->save();
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::MarkToJSON($markDB);
    }

    //Удаление метки
    public function actionDeleteMark() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $mark_id = $request->post('mark_id');
        self::checkEmpty($user_id, $mark_id);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);

        $markDB = Mark::find()->where(['id' => $mark_id])->one();
        if (!isset($markDB))
            throw new NotFoundHttpException($message = "Метка не найдена", $code = 404);
        if ($markDB->author != $userDB->id)
            throw new ForbiddenHttpException($message = "Удалить метку может только её автор!", $code = 403);
        $date = new DateTime;
        $date2 = new DateTime($markDB->date);
        if ($date->sub(new DateInterval("PT24H")) > $date2)
            throw new \yii\web\HttpException(408); //удаление метки доступно в течение 24 часов после добавления
        $markDB->delete();
    }

    //Получить все метки в заданной точке
    public function actionGetMarks() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $latitude = $request->post('latitude');
        $longitude = $request->post('longitude');
        $deltax = $request->post('accuracy');
        if(!isset($deltax) || $deltax<20)$deltax=20;
        $deltax/=100000;
        self::checkEmpty($user_id, $latitude, $longitude);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
       $query = "(POW({{coordinate}}.[[latitude]] - $latitude, 2)+POW({{coordinate}}.[[longitude]]-$longitude,2))";
        $marks = Mark::find()->select(['{{mark}}.*', "$query AS distance"])->where(['<=', $query, $deltax**2])->orderBy(['isEncrypted' => SORT_DESC,'distance' => SORT_ASC])->joinWith('coords')->all();
        foreach ($marks as &$mark) {
            if ($mark->isAnonymed)
                $mark->author = 0;
            $mark = self::MarkToJSON($mark);
        }
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return $marks;
    }

    //Отправка нового сообщения
    public function actionNewMessage() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $message = json_decode(file_get_contents('php://input'));
        self::checkEmpty($user_id, $message);
        $group = null;
        $mark = null;
        if (isset($message->{'groupReceiver'}->{'id'})) {
            $group = Group::findOne($message->{'groupReceiver'}->{'id'});
        }
        if (isset($message->{'mark'}->{'id'})) {
            $mark = Mark::findOne($message->{'mark'}->{'id'});
        }
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $mes = new Message;
        $mes->message = $message->{'message'};
        $mes->link('author', $userDB);
        if (isset($group))
            $mes->link('groupReceiver', $group);
        if (isset($mark))
            $mes->link('mark', $mark);
        $date = new DateTime;
        $mes->date = $date->format('Y-m-d H:i');
        $mes->save();
        if(isset($message->{'receivers'})){
        foreach ($message->{'receivers'} as $receiver) {
            $receiverDB = User::findOne($receiver->id);
            $mes->link('receivers', $receiverDB);
        }
        }
if(isset($message->{'key'})){
        foreach ($message->{'key'} as $key => $value) {
            $mes_key = new MessageKeys;
            $keyDB = Key::findOne($key);
            $mes_key->encrypted_key = $value;
            $mes_key->key_id = $keyDB->id;
            $mes->link('keys', $mes_key);
            $mes_key->save();
        }
}
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        $mes = self::MessageToJSON($mes);
        return $mes;
    }

    //Создание новой группы
    public function actionAddGroup() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $group = json_decode(file_get_contents('php://input'));
        self::checkEmpty($user_id, $group);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $groupDB = new Group;
        $groupDB->name = $group->{'name'};
        $groupDB->save();
        $groupDB->link('administrators', $userDB);
        foreach ($group->{'members'} as $member) {
            $memberDB = User::findOne($member->id);
            $groupDB->link('members', $memberDB);
        }
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::GroupToJSON($groupDB);
    }

    //Изменение параметров группы
    public function actionChangeGroup() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $group = json_decode(file_get_contents('php://input'));
        self::checkEmpty($user_id, $group);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $groupDB = Group::find()->where(['groups.id' => $group->{'id'}, 'group_admin.admin_id' => $userDB->id])->joinWith('administrators')->joinWith('members')->one();
        if (!isset($groupDB))
            throw new NotFoundHttpException($message = "Группа не найдена!", $code = 404);
        $groupDB->name = $group->{'name'};
        $groupDB->save();
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::GroupToJSON($groupDB);
    }

    //Добавление администратора в группу
    public function actionAddAdministrator() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $group_id = $request->post('group_id');
        $admin_id = $request->post('admin_id');
        self::checkEmpty($user_id, $group_id, $admin_id);
        $userDB = self::getUser($user_id);
       self::checkCookie($userDB->id);
        $groupDB = Group::find()->where(['groups.id' => $group_id, 'group_admin.admin_id' => $userDB->id, 'group_members.user_id' => $admin_id])->joinWith('administrators')->joinWith('members')->one();
        if (!isset($groupDB))
            throw new BadRequestHttpException($message = "Неправильный запрос", $code = 400);
        $adminDB = self::getUser($admin_id);
        $groupDB->link('administrators', $adminDB);
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::GroupToJSON($groupDB);
    }

    //Удаление юзера из списка администраторов
    public function actionDeleteAdministrator() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $group_id = $request->post('group_id');
        $admin_id = $request->post('admin_id');
        self::checkEmpty($user_id, $group_id, $admin_id);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $groupDB = Group::find()->where(['groups.id' => $group_id, 'group_admin.admin_id' => $userDB->id, 'group_admin.admin_id' => $admin_id])->joinWith('administrators')->joinWith('members')->one();
        if (!isset($groupDB))
            throw new BadRequestHttpException($message = "Неправильный запрос", $code = 400);
        $adminDB = self::getUser($admin_id);
        $groupDB->unlink('administrators', $adminDB, true);
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::GroupToJSON($groupDB);
    }

    //Добавление нового участника в группу
    public function actionAddMember() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $group_id = $request->post('group_id');
        $member_id = $request->post('member_id');
        self::checkEmpty($user_id, $group_id, $member_id);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $groupDB = Group::find()->where(['groups.id' => $group_id, 'group_admin.admin_id' => $userDB->id])->joinWith('administrators')->joinWith('members')->one();
        if (!isset($groupDB))
            throw new BadRequestHttpException($message = "Неправильный запрос", $code = 400);
        $memberDB = self::getUser($member_id);
        $groupDB->link('members', $memberDB);
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::GroupToJSON($groupDB);
    }

    //Удаление участника из группы
    public function actionDeleteMember() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $group_id = $request->post('group_id');
        $member_id = $request->post('member_id');
        self::checkEmpty($user_id, $group_id, $member_id);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $groupDB = Group::find()->where(['group_admin.admin_id' => $userDB->id])->orWhere("$user_id=$member_id")->andWhere(['not in', 'group_admin.admin_id', $member_id])->andWhere(['groups.id' => $group_id])->joinWith('administrators')->joinWith('members')->one();
        if (!isset($groupDB))
            throw new BadRequestHttpException($message = "Неправильный запрос", $code = 400);
        $memberDB = self::getUser($member_id);
        $groupDB->unlink('members', $memberDB, true);
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return self::GroupToJSON($groupDB);
    }

    //Получить группы текущего пользователя
    public function actionGetGroups() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        self::checkEmpty($user_id);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $groups = Group::find()->where(['group_members.user_id' => $user_id])->joinWith('members')->joinWith('administrators')->all();
        foreach ($groups as &$group) {
            $group = self::GroupToJSON($group);
        }
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return $groups;
    }

    //Получить сообщения для пользователя(отправленные и принятые) и групп, в которых он состоит
    public function actionGetMessages() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        self::checkEmpty($user_id);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $messages = Message::find()->where(['message.author' => $userDB->id])->orWhere("message_receiver.receiver_id= $userDB->id")->orWhere(['group_members.user_id' => $userDB->id])->orderBy(['id' => SORT_DESC])->joinWith('receivers')->joinWith('keys')->joinWith('groupReceiver')->joinWith('groupReceiver.members')->joinWith('author')->all();
        if (isset($messages)) {
            foreach ($messages as &$mes) {
                $mes = self::MessageToJSON($mes);
            }
        }
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return $messages;
    }
        public function actionGetMessagesWith() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $sub_id = $request->get('sub_id');
        $sub = $request->get('sub');
        self::checkEmpty($user_id, $sub_id, $sub);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        switch($sub){
            case "group":
                        $messages = Message::find()->where(['groupReceiver' => $sub_id])->andWhere(['group_members.user_id' => $userDB->id])->orderBy(['id' => SORT_DESC])->joinWith('keys')->joinWith('groupReceiver')->joinWith('groupReceiver.members')->joinWith('author')->all();
                break;
            case "user":
                             $messages = Message::find()->where("message.author=$userDB->id")->andWhere("message_receiver.receiver_id= $sub_id")->orWhere(['and', "message.author=$sub_id", "message_receiver.receiver_id= $userDB->id"])->orderBy(['id' => SORT_DESC])->joinWith('receivers')->joinWith('keys')->joinWith('author')->all();
                break;
                default:
                    $messages = null;
        }

        if (isset($messages)) {
            foreach ($messages as &$mes) {
                $mes = self::MessageToJSON($mes);
            }
        }
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return $messages;
    }

    //Выйти с текущего устройства
    public function actionExit() {
        $request = Yii::$app->request;
       $user_id = $request->get('user_id');
        $key_id = $request->post('key_id');
        self::checkEmpty($user_id,$key_id);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $key = Key::findOne($key_id);
        if (isset($key))
            $key->delete();
        $cookies = Yii::$app->request->cookies;
          $cookie=explode("/", $cookies->getValue('session_key', 'null'));
        if(count($cookie)==2){
        $s_id = $cookie[0];
 $cookies = Yii::$app->response->cookies;
        $cookies->remove('session_key');
        $login = Login::find()->where(['id' => $s_id, 'user_id' => $userDB->id])->one();
        $login->session_key = "";
        $login->save();
        }
    }

    //Выйти со всех устройств, кроме текущего
    public function actionExitAll() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $key_id = $request->post('key_id');
        self::checkEmpty($user_id, $key_id);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $key = Key::findOne($key_id);
        $cookies = Yii::$app->request->cookies;
          $cookie=explode("/", $cookies->getValue('session_key', 'null'));
        if(count($cookie)==2){
        $s_id = $cookie[0];
        if (isset($key))
            Key::deleteAll(['and', "user_id=$userDB->id", ['not in', 'id', $key->id]]);
        $logins = Login::find()->where(['user_id' => $userDB->id])->andWhere(['not in', 'id', $s_id])->all();
        foreach ($logins as &$login) {
            $login->session_key = "";
            $login->save();
        }
    }
    }

    //Поиск пользователя
    public function actionSearchUser() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        $str = $request->post('str');
        self::checkEmpty($user_id, $str);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $str_array = explode(" ", $str);
        $users = User::find()->where(['in', 'user.firstName', $str_array])->orWhere(['in', 'user.lastName', $str_array])->orWhere(['in', 'user.email', $str_array])->joinWith('friends')->joinWith('keys')->all();
        foreach ($users as &$user) {
            $user = self::getUser($user);
        }
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return $users;
    }
    
    //Список взаимных друзей
    public function actionGetFriends() {
        $request = Yii::$app->request;
        $user_id = $request->get('user_id');
        self::checkEmpty($user_id);
        $userDB = self::getUser($user_id);
        self::checkCookie($userDB->id);
        $users = User::find()->where(['in', 'friends.friends_id', $userDB->id])->joinWith('friends')->joinWith('keys')->all();
        $users = array_intersect($users, $userDB->friends);
        foreach ($users as &$user) {
            $user = self::JSONUser($user);
        }
        Yii::$app->response->format = \yii\web\Response::FORMAT_JSON;
        return $users;
    }

    /*  СЛУЖЕБНЫЕ ФУНКЦИИ   */
    /*  Предназначены для работы внутри класса-контроллера  */

    //проверка Cookie
    private static function checkCookie($user_id) {
        $cookies = Yii::$app->request->cookies;
        $cookie=explode("/", $cookies->getValue('session_key', 'null'));
        if(count($cookie)==2){
        $s_key = $cookie[1];
        $s_id = $cookie[0];
        
        $login = Login::find()->where(['user_id' => $user_id, 'id' => $s_id])->one();
        }
        if (!isset($login) || !(password_verify($s_key, $login->session_key)) || count($cookie)!=2)
            throw new UnauthorizedHttpException($message = "Неверный ключ сессии", $code = 401);
    }

    //Получение юзера по id или email
    private static function getUser($user_id = 0, $email = "null") {
        $userDB = null;
        $userDB = User::find()->andWhere(['user.id' => $user_id])->orWhere(['user.email' => $email])->joinWith(['keys', 'friends'])->one();
        if (!isset($userDB))
            throw new NotFoundHttpException($message = "Пользователь не найден", $code = 404);
        return $userDB;
    }

    //проверка передаваемых через POST параметров на пустоту
    private static function checkEmpty(...$args) {
        foreach ($args as $arg) {
            if (!isset($arg))
                throw new BadRequestHttpException($message = "Параметр не заполнен", $code = 400);
        }
    }

    //преобразование объектов со всеми связями в строку JSON
    private static function JSONUser(User $user) {
        $arr = array('id' => $user->id, 'email' => $user->email, 'firstName' => $user->firstName, 'lastName' => $user->lastName, 'phoneNumber' => $user->phoneNumber, 'keys' => $user->keys, 'friends' => $user->friends);
        return $arr;
    }

    private static function MarkToJSON(Mark $mark) {
        if ($mark->author != 0)
            $author = self::getUser($mark->author);
        else
            $author = null;
            $date = new DateTime($mark->date);
        $arr = array('id' => $mark->id, 'message' => $mark->message, 'author' => $author, 'isAnonymed' => (boolean)$mark->isAnonymed, 'isEncrypted' => (boolean)$mark->isEncrypted, 'date' => $date->getTimestamp(), 'coords' => Coordinate::findOne($mark->coords));
        return $arr;
    }

    private static function MessageToJSON(Message $message) {
        $date = new DateTime($message->date);
        $keys=array();
        foreach($message->keys as $key){
            $keys[$key->key_id] = $key->encrypted_key;
        }
        if(Group::findOne($message->groupReceiver)!==null){
            $group = self::GroupToJSON(Group::findOne($message->groupReceiver));
        }else $group = null;
        if(Mark::findOne($message->mark)!==null){
            $mark = self::MarkToJSON(Mark::findOne($message->mark));
        } else $mark = null;
        $arr = array('id' => $message->id, 'message' => $message->message, 'author' => self::getUser($message->author), 'groupReceiver' => $group, 'date' => $date->getTimestamp(), 'mark' => $mark, 'receivers' => $message->receivers, 'key' => $keys);
        return $arr;
    }

    private static function GroupToJSON(Group $group) {
        $members = $group->members;
                foreach ($members as &$user) {
            $user = self::JSONUser($user);
        }
        $arr = array('id' => $group->id, 'name' => $group->name, 'administrators' => $group->administrators, 'members' => $members);
        return $arr;
    }
	
	//генерация ключа сессии
   private static function generateKey($length = 32){
  $chars = 'qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789';
  $numChars = strlen($chars);
  $string = '';
  for ($i = 0; $i < $length; $i++) {
    $string .= substr($chars, random_int(1, $numChars) - 1, 1);
  }
  return $string;
}

}
?>