#!/bin/bash

DB_PATH=../advanced-optics.db

rm $DB_PATH
sqlite3 $DB_PATH < ../schema.sql