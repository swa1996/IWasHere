<?php

namespace app\models;
use yii\db\ActiveRecord;

class Group extends ActiveRecord
{
public static function tableName(){
    return "groups";
}
public function getAdministrators()
{
    return $this->hasMany(User::className(), ['id' => 'admin_id'])
        ->viaTable('group_admin', ['group_id' => 'id'])->from(['u2' => User::tableName()]);
}
public function getMembers()
{
    return $this->hasMany(User::className(), ['id' => 'user_id'])
        ->viaTable('group_members', ['group_id' => 'id'])->from(['u3' => User::tableName()]);
}
public function setAdministratots($adm){
    $this->administrators = $adm;
}
public function setMembers($members){
    $this->members = $members;
}
}
?>