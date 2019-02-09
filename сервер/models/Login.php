<?php

namespace app\models;
use yii\db\ActiveRecord;

class Login extends ActiveRecord
{
public static function tableName(){
    return "login";
}
public function getUser()
{
    return $this->hasOne(User::className(), ['id' => 'user_id']);
}
public function setUser(User $user){
    $this->user_id = $user->id;
}
}
?>