#!/usr/bin/env python3.10

import tkinter as tk
from tkinter import ttk
import os

FIFO_PATH = '/tmp/mc-deck-debug-sim'

try:
	os.unlink(FIFO_PATH)
except FileNotFoundError:
	pass
os.mkfifo(FIFO_PATH)

# simpipe = open(FIFO_PATH, 'w')
# simpipe.write('asdfasdf')

rootwin = tk.Tk()
canvas = tk.Canvas(rootwin, bg='white', width=800, height=600)

canvas.create_rectangle(10, 10, 110, 60, outline='black', tags='left_analog')
l_trig_text = canvas.create_text(60, 35, text="LT = XXXXX", fill='black', tags='left_analog')

canvas.create_rectangle(10, 70, 110, 120, outline='black', tags='left_digital')
canvas.create_text(60, 95, text="LB", fill='black', tags='left_digital')

canvas.create_rectangle(60, 130, 110, 180, outline='black', tags='d_up')
canvas.create_text(85, 155, text="UP", fill='black', tags='d_up')
canvas.create_rectangle(10, 180, 60, 230, outline='black', tags='d_left')
canvas.create_text(35, 205, text="LEFT", fill='black', tags='d_left')
canvas.create_rectangle(110, 180, 160, 230, outline='black', tags='d_right')
canvas.create_text(135, 205, text="RIGHT", fill='black', tags='d_right')
canvas.create_rectangle(60, 230, 110, 280, outline='black', tags='d_down')
canvas.create_text(85, 255, text="DOWN", fill='black', tags='d_down')

canvas.create_rectangle(170, 130, 370, 330, outline='black', tags='l_thumb')
l_thumb_text = canvas.create_text(270, 230, text="L Thumb = (XXXXX, YYYYY)", fill='black', tags='l_thumb')

canvas.create_rectangle(170, 340, 370, 540, outline='black', tags='l_pad')
l_pad_text = canvas.create_text(270, 440, text="L Pad = (XXXXX, YYYYY)", fill='black', tags='l_pad')

canvas.create_rectangle(170, 10, 220, 60, outline='black', tags='btn_view')
canvas.create_text(195, 35, text="VIEW", fill='black', tags='btn_view')
canvas.create_rectangle(10, 340, 60, 390, outline='black', tags='btn_steam')
canvas.create_text(35, 365, text="STM", fill='black', tags='btn_steam')
canvas.create_rectangle(10, 400, 60, 450, outline='black', tags='btn_l4')
canvas.create_text(35, 425, text="L4", fill='black', tags='btn_l4')
canvas.create_rectangle(10, 460, 60, 510, outline='black', tags='btn_l5')
canvas.create_text(35, 485, text="L5", fill='black', tags='btn_l5')

canvas.create_rectangle(640, 10, 740, 60, outline='black', tags='right_analog')
r_trig_text = canvas.create_text(690, 35, text="RT = XXXXX", fill='black', tags='right_analog')

canvas.create_rectangle(640, 70, 740, 120, outline='black', tags='right_digital')
canvas.create_text(690, 95, text="RB", fill='black', tags='right_digital')

canvas.create_rectangle(640, 130, 690, 180, outline='black', tags='btn_y')
canvas.create_text(665, 155, text="Y", fill='black', tags='btn_y')
canvas.create_rectangle(590, 180, 640, 230, outline='black', tags='btn_x')
canvas.create_text(615, 205, text="X", fill='black', tags='btn_x')
canvas.create_rectangle(690, 180, 740, 230, outline='black', tags='btn_b')
canvas.create_text(715, 205, text="B", fill='black', tags='btn_b')
canvas.create_rectangle(640, 230, 690, 280, outline='black', tags='btn_a')
canvas.create_text(665, 255, text="A", fill='black', tags='btn_a')

canvas.create_rectangle(380, 130, 580, 330, outline='black', tags='r_thumb')
r_thumb_text = canvas.create_text(480, 230, text="R Thumb = (XXXXX, YYYYY)", fill='black', tags='r_thumb')

canvas.create_rectangle(380, 340, 580, 540, outline='black', tags='r_pad')
r_pad_text = canvas.create_text(480, 440, text="R Pad = (XXXXX, YYYYY)", fill='black', tags='r_pad')

canvas.create_rectangle(530, 10, 580, 60, outline='black', tags='btn_option')
canvas.create_text(555, 35, text="OPTN", fill='black', tags='btn_option')
canvas.create_rectangle(690, 340, 740, 390, outline='black', tags='btn_dots')
canvas.create_text(715, 365, text="...", fill='black', tags='btn_dots')
canvas.create_rectangle(690, 400, 740, 450, outline='black', tags='btn_r4')
canvas.create_text(715, 425, text="R4", fill='black', tags='btn_r4')
canvas.create_rectangle(690, 460, 740, 510, outline='black', tags='btn_r5')
canvas.create_text(715, 485, text="R5", fill='black', tags='btn_r5')

canvas.pack()
rootwin.mainloop()
