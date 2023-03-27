#!/usr/bin/env python3.10

import threading
import tkinter as tk
from tkinter import ttk
import os
import struct
import time

FIFO_PATH = '/tmp/mc-deck-debug-sim'

try:
	os.unlink(FIFO_PATH)
except FileNotFoundError:
	pass
os.mkfifo(FIFO_PATH)

pad_state = {
	'd_up': False,
	'd_down': False,
	'd_left': False,
	'd_right': False,
	'btn_a': False,
	'btn_b': False,
	'btn_x': False,
	'btn_y': False,
	'left_digital': False,
	'right_digital': False,
	'btn_view': False,
	'btn_steam': False,
	'btn_option': False,
	'btn_dots': False,
	'btn_l4': False,
	'btn_l5': False,
	'btn_r4': False,
	'btn_r5': False,
}
l_trig_val = 0
r_trig_val = 0

touch_sticks_state = {
	'l_thumb': None,
	'r_thumb': None,
	'l_pad': None,
	'r_pad': None,
}

TOUCH_STICKS_DATA = {
	'l_thumb': ((170, 130, 370, 330), "L Thumb = ({}, {})"),
	'r_thumb': ((380, 130, 580, 330), "R Thumb = ({}, {})"),
	'l_pad': ((170, 340, 370, 540), "L Pad = ({}, {})"),
	'r_pad': ((380, 340, 580, 540), "R Pad = ({}, {})"),
}

iothread_run = True
def iothread_func():
	simpipe = open(FIFO_PATH, 'wb')
	frame_id = 0

	while iothread_run:
		# print(pad_state, l_trig_val, r_trig_val, touch_sticks_state)
		buttons0 = (
			((r_trig_val > 16384) << 0) |
			((l_trig_val > 16384) << 1) |
			(pad_state['right_digital'] << 2) |
			(pad_state['left_digital'] << 3) |
			(pad_state['btn_y'] << 4) |
			(pad_state['btn_b'] << 5) |
			(pad_state['btn_x'] << 6) |
			(pad_state['btn_a'] << 7) |
			(pad_state['d_up'] << 8) |
			(pad_state['d_right'] << 9) |
			(pad_state['d_left'] << 10) |
			(pad_state['d_down'] << 11) |
			(pad_state['btn_view'] << 12) |
			(pad_state['btn_steam'] << 13) |
			(pad_state['btn_option'] << 14) |
			(pad_state['btn_l5'] << 15) |
			(pad_state['btn_r5'] << 16) |
			# no support for pad press
			((touch_sticks_state['l_pad'] is not None) << 19) |
			((touch_sticks_state['r_pad'] is not None) << 20)
			# no support for thumbstick click
		)
		buttons1 = (
			(pad_state['btn_l4'] << 9) |
			(pad_state['btn_r4'] << 10) |
			((touch_sticks_state['l_thumb'] is not None) << 14) |
			((touch_sticks_state['r_thumb'] is not None) << 15) |
			(pad_state['btn_dots'] << 18)
		)
		pkt = struct.pack(
			"<BBBBIIIhhhhhhhhhhhhhhHHhhhhhhhh",
			1,
			0,
			9,
			0x40,

			frame_id,
			buttons0,
			buttons1,

			0 if touch_sticks_state['l_pad'] is None else touch_sticks_state['l_pad'][0],
			0 if touch_sticks_state['l_pad'] is None else touch_sticks_state['l_pad'][1],
			0 if touch_sticks_state['r_pad'] is None else touch_sticks_state['r_pad'][0],
			0 if touch_sticks_state['r_pad'] is None else touch_sticks_state['r_pad'][1],

			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,	# no support for motion controls

			l_trig_val,
			r_trig_val,

			0 if touch_sticks_state['l_thumb'] is None else touch_sticks_state['l_thumb'][0],
			0 if touch_sticks_state['l_thumb'] is None else touch_sticks_state['l_thumb'][1],
			0 if touch_sticks_state['r_thumb'] is None else touch_sticks_state['r_thumb'][0],
			0 if touch_sticks_state['r_thumb'] is None else touch_sticks_state['r_thumb'][1],

			0, 0, 0, 0	# no support for force sensors
		)
		frame_id = (frame_id + 1) & 0xFFFFFFFF

		assert len(pkt) == 64
		# print(pkt)
		simpipe.write(pkt)

		time.sleep(0.004)

iothread = threading.Thread(target=iothread_func)
iothread.start()

rootwin = tk.Tk()
canvas = tk.Canvas(rootwin, bg='white', width=800, height=600)

canvas.create_rectangle(10, 10, 110, 60, outline='black', fill='white', tags='left_analog')
l_trig_text = canvas.create_text(60, 35, text="LT = 0", fill='black', tags='left_analog')

canvas.create_rectangle(10, 70, 110, 120, outline='black', fill='white', tags='left_digital')
canvas.create_text(60, 95, text="LB", fill='black', tags='left_digital')

canvas.create_rectangle(60, 130, 110, 180, outline='black', fill='white', tags='d_up')
canvas.create_text(85, 155, text="UP", fill='black', tags='d_up')
canvas.create_rectangle(10, 180, 60, 230, outline='black', fill='white', tags='d_left')
canvas.create_text(35, 205, text="LEFT", fill='black', tags='d_left')
canvas.create_rectangle(110, 180, 160, 230, outline='black', fill='white', tags='d_right')
canvas.create_text(135, 205, text="RIGHT", fill='black', tags='d_right')
canvas.create_rectangle(60, 230, 110, 280, outline='black', fill='white', tags='d_down')
canvas.create_text(85, 255, text="DOWN", fill='black', tags='d_down')

canvas.create_rectangle(TOUCH_STICKS_DATA['l_thumb'][0], outline='black', fill='white', tags='l_thumb')
l_thumb_text = canvas.create_text(270, 230, text=TOUCH_STICKS_DATA['l_thumb'][1].format(0, 0), fill='black', tags='l_thumb')

canvas.create_rectangle(TOUCH_STICKS_DATA['l_pad'][0], outline='black', fill='white', tags='l_pad')
l_pad_text = canvas.create_text(270, 440, text=TOUCH_STICKS_DATA['l_pad'][1].format(0, 0), fill='black', tags='l_pad')

canvas.create_rectangle(170, 10, 220, 60, outline='black', fill='white', tags='btn_view')
canvas.create_text(195, 35, text="VIEW", fill='black', tags='btn_view')
canvas.create_rectangle(10, 340, 60, 390, outline='black', fill='white', tags='btn_steam')
canvas.create_text(35, 365, text="STM", fill='black', tags='btn_steam')
canvas.create_rectangle(10, 400, 60, 450, outline='black', fill='white', tags='btn_l4')
canvas.create_text(35, 425, text="L4", fill='black', tags='btn_l4')
canvas.create_rectangle(10, 460, 60, 510, outline='black', fill='white', tags='btn_l5')
canvas.create_text(35, 485, text="L5", fill='black', tags='btn_l5')

canvas.create_rectangle(640, 10, 740, 60, outline='black', fill='white', tags='right_analog')
r_trig_text = canvas.create_text(690, 35, text="RT = 0", fill='black', tags='right_analog')

canvas.create_rectangle(640, 70, 740, 120, outline='black', fill='white', tags='right_digital')
canvas.create_text(690, 95, text="RB", fill='black', tags='right_digital')

canvas.create_rectangle(640, 130, 690, 180, outline='black', fill='white', tags='btn_y')
canvas.create_text(665, 155, text="Y", fill='black', tags='btn_y')
canvas.create_rectangle(590, 180, 640, 230, outline='black', fill='white', tags='btn_x')
canvas.create_text(615, 205, text="X", fill='black', tags='btn_x')
canvas.create_rectangle(690, 180, 740, 230, outline='black', fill='white', tags='btn_b')
canvas.create_text(715, 205, text="B", fill='black', tags='btn_b')
canvas.create_rectangle(640, 230, 690, 280, outline='black', fill='white', tags='btn_a')
canvas.create_text(665, 255, text="A", fill='black', tags='btn_a')

canvas.create_rectangle(TOUCH_STICKS_DATA['r_thumb'][0], outline='black', fill='white', tags='r_thumb')
r_thumb_text = canvas.create_text(480, 230, text=TOUCH_STICKS_DATA['r_thumb'][1].format(0, 0), fill='black', tags='r_thumb')

canvas.create_rectangle(TOUCH_STICKS_DATA['r_pad'][0], outline='black', fill='white', tags='r_pad')
r_pad_text = canvas.create_text(480, 440, text=TOUCH_STICKS_DATA['r_pad'][1].format(0, 0), fill='black', tags='r_pad')

canvas.create_rectangle(530, 10, 580, 60, outline='black', fill='white', tags='btn_option')
canvas.create_text(555, 35, text="OPTN", fill='black', tags='btn_option')
canvas.create_rectangle(690, 340, 740, 390, outline='black', fill='white', tags='btn_dots')
canvas.create_text(715, 365, text="...", fill='black', tags='btn_dots')
canvas.create_rectangle(690, 400, 740, 450, outline='black', fill='white', tags='btn_r4')
canvas.create_text(715, 425, text="R4", fill='black', tags='btn_r4')
canvas.create_rectangle(690, 460, 740, 510, outline='black', fill='white', tags='btn_r5')
canvas.create_text(715, 485, text="R5", fill='black', tags='btn_r5')

touch_sticks_ids = {
	'l_thumb': l_thumb_text,
	'r_thumb': r_thumb_text,
	'l_pad': l_pad_text,
	'r_pad': r_pad_text,
}

for button in ['d_up', 'd_down', 'd_left', 'd_right', 'btn_a', 'btn_b', 'btn_x', 'btn_y', 'left_digital', 'right_digital', 'btn_view', 'btn_steam', 'btn_option', 'btn_dots', 'btn_l4', 'btn_l5', 'btn_r4', 'btn_r5']:
	def f(button):
		canvas.tag_bind(button, '<Button-1>', lambda _: pad_state.update({button: True}))
		canvas.tag_bind(button, '<ButtonRelease-1>', lambda _: pad_state.update({button: False}))
	f(button)

def l_trig_motion(e):
	global l_trig_val
	x = e.x - 10
	if x < 0:
		x = 0
	if x > 100:
		x = 100
	x = x * 32767.0 / 100.0
	l_trig_val = int(x)
	canvas.itemconfig(l_trig_text, text=f"LT = {l_trig_val}")
def l_trig_release(_):
	global l_trig_val
	l_trig_val = 0
	canvas.itemconfig(l_trig_text, text=f"LT = {l_trig_val}")
canvas.tag_bind('left_analog', '<B1-Motion>', l_trig_motion)
canvas.tag_bind('left_analog', '<ButtonRelease-1>', l_trig_release)
def r_trig_motion(e):
	global r_trig_val
	x = e.x - 640
	if x < 0:
		x = 0
	if x > 100:
		x = 100
	x = x * 32767.0 / 100.0
	r_trig_val = int(x)
	canvas.itemconfig(r_trig_text, text=f"RT = {r_trig_val}")
def r_trig_release(_):
	global r_trig_val
	r_trig_val = 0
	canvas.itemconfig(r_trig_text, text=f"RT = {r_trig_val}")
canvas.tag_bind('right_analog', '<B1-Motion>', r_trig_motion)
canvas.tag_bind('right_analog', '<ButtonRelease-1>', r_trig_release)

for thumb_pad in ['l_thumb', 'r_thumb', 'l_pad', 'r_pad']:
	def f(thumb_pad):
		def update_state(e):
			x = e.x - TOUCH_STICKS_DATA[thumb_pad][0][0]
			y = e.y - TOUCH_STICKS_DATA[thumb_pad][0][1]
			if x < 0:
				x = 0
			if x > 200:
				x = 200
			if y < 0:
				y = 0
			if y > 200:
				y = 200
			x = x * 65535.0 / 200.0 - 32767
			y = 32767 - y * 65535.0 / 200.0
			x = int(x)
			y = int(y)
			touch_sticks_state[thumb_pad] = (x, y)
			canvas.itemconfig(touch_sticks_ids[thumb_pad], text=TOUCH_STICKS_DATA[thumb_pad][1].format(x, y))
		canvas.tag_bind(thumb_pad, '<Button-1>', update_state)
		canvas.tag_bind(thumb_pad, '<B1-Motion>', update_state)
		canvas.tag_bind(thumb_pad, '<ButtonRelease-1>', lambda _: touch_sticks_state.update({thumb_pad: None}))
	f(thumb_pad)

canvas.pack()
rootwin.mainloop()
iothread_run = False
