#!/usr/bin/env python2
from __future__ import unicode_literals

import os
from PIL import Image
import argparse
import xml.etree.ElementTree as ElementTree
import io

parser = argparse.ArgumentParser(prog='gen-sprite', description="""Generate sprites from extracted emojis.""")
parser.add_argument('-e', '--emojis', help='folder where emojis are stored', default='output/', required=False)
parser.add_argument('-i', '--xml', help='XML containing emojis map', default='emoji-categories.xml', required=False)
parser.add_argument('-s', '--size', help='Maximum number of emojis per line', default='15', required=False)
parser.add_argument('-r', '--resize', help='Maximum width for sprites', default='1530', required=False)
args = parser.parse_args()

xml = ElementTree.parse(args.xml).getroot()
parsedItems = []

for group in xml:
    groupName = group.attrib['name']
    emojis = []
    output = io.open(groupName + '.txt', 'w', encoding='utf8')
    for item in group:
        emoji = item.text.replace(',', '_u').lower()
        if '|' not in emoji and os.path.isfile(args.emojis + 'emoji_u' + emoji + '.png') and emoji not in parsedItems:
            parsedItems.append(emoji)
            emojis.append(args.emojis + 'emoji_u' + emoji + '.png')
            emojiCodePoint = '\\U' + emoji.replace('_u', '\\U')
            emojiCodePoint = emojiCodePoint.split('\\U')
            finalCodePoints = []
            for point in emojiCodePoint:
                point = point.replace('\\U', '')
                if len(point) > 0:
                    if len(point) < 8:
                        point = "0" * (8 - len(point)) + point
                    finalCodePoints.append(point)
            char = '\\U' + '\\U'.join(finalCodePoints)
            output.write(char.decode('unicode_escape') + '\n')
    images = [Image.open(filename) for filename in emojis]
    output.close()


    if len(images) > 0:
        print "Generating sprite for " + groupName
        masterWidth  = (128 * int(args.size))
        lines = float(len(images)) / float(args.size)
        if not lines.is_integer():
            lines += 1
            lines = int(lines)
        masterHeight = int(128 * lines)
        master = Image.new(
            mode='RGBA',
            size=(masterWidth, masterHeight),
            color=(0,0,0,0)
        )

        offset = -1
        for count, image in enumerate(images):
            location = 128 * count % masterWidth
            if location == 0:
                offset += 1
                location = 0
            master.paste(image, (location, 128 * offset))
        ratio = float(masterWidth) / float(args.resize)
        newHeight = int(masterHeight / ratio)
        master = master.resize((int(args.resize), newHeight))
        master.save(groupName + '.png', 'PNG')
    else:
        print 'Ignoring ' + groupName + '...'
