#!/usr/bin/env python3

import os

FIFO_PATH = '/tmp/mc-deck-debug-sim'

try:
	os.unlink(FIFO_PATH)
except FileNotFoundError:
	pass
os.mkfifo(FIFO_PATH)

simpipe = open(FIFO_PATH, 'w')
simpipe.write('asdfasdf')
