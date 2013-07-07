<?php
class Device extends Zend_Db_Table_Row {
	
	protected function _insert() {
		parent::_insert();
		$this->created = date("Y-m-d H:i:s");
		$this->generateHash("web_id");
	}
	
	public function generateHash($col, $extension = "") {
		do {
			$this->$col = sha1("viser#" . microtime()) . $extension;
			$others = $this->getTable()->fetchRow(array("{$col} = ?" => $this->$col));
		}
		while ($others != null);
	}
	
	
}