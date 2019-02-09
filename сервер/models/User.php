<?php

namespace app\models;
use yii\db\ActiveRecord;

class User extends ActiveRecord
{

public function getKeys()
{
    return $this->hasMany(Key::className(), ['user_id' => 'id']);
}
public function getFriends()
{
    return $this->hasMany(User::className(), ['id' => 'friends_id'])
        ->viaTable('friends', ['user_id' => 'id'])->from(['u2' => User::tableName()]);
}
public function setKeys( $keys){
    $this->keys = $keys;
}
public function setFriends( $friends){
    $this->friends = $friends;
}
    public function __toString()
    {
        return "userID: $this->id ";
    }
}
?>