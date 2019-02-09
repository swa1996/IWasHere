<?php

namespace app\models;
use yii\db\ActiveRecord;

class Key extends ActiveRecord
{
public static function tableName(){
    return "pkeys";
}
public function getUser()
{
    return $this->hasOne(User::className(), ['id' => 'user_id']);
}
public function setUser(User $user){
$this->user = $user;    
}
}
?>