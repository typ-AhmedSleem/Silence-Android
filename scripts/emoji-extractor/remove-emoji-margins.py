#!/usr/bin/env python2

import os
import argparse
from PIL import Image, ImageChops

parser = argparse.ArgumentParser(prog='emoji-extractor', description="""Resize extracted emojis to 128x128.""")
parser.add_argument('-e', '--emojis', help='folder where emojis are stored', default='output/', required=False)
args = parser.parse_args()

for element in os.listdir(args.emojis):
    imagePath = os.path.abspath(args.emojis + element)
    try:
        print 'Cropping ' + element + '...'
        image = Image.open(imagePath)
        width, height = image.size
        box = (4, 0, width-4, height)
        crop = image.crop(box)
        crop.save(imagePath)
    except:
        print 'Cannot crop emoji_' + name + '.png...'
