#!/usr/bin/env python2

import os
import shutil
import argparse
from fontTools.ttLib import TTFont
import xml.etree.ElementTree as ElementTree
import binascii
import glob

parser = argparse.ArgumentParser(prog='emoji-extractor', description="""Extract emojis from NotoColorEmoji.ttf. Requires FontTools.""")
parser.add_argument('-i', '--input', help='the TTF file to parse', default='NotoColorEmoji.ttf', required=False)
parser.add_argument('-o', '--output', help='the png output folder', default='output/', required=False)
args = parser.parse_args()

try:
    shutil.rmtree(args.output)
except:
    pass

os.mkdir(args.output)

font = TTFont(args.input)
font.saveXML('.NotoColorEmoji.ttx')

ttx = ElementTree.parse('.NotoColorEmoji.ttx').getroot()
os.remove('.NotoColorEmoji.ttx')

for element in ttx.find('CBDT').find('strikedata'):
    data = element.find('rawimagedata').text.split()
    name = element.attrib['name'].lower()
    name = name.replace('uni', 'u')
    imagePath = os.path.abspath(args.output + 'emoji_' + name + '.png')
    print 'Extracting emoji_' + name + '.png'
    emoji = open(imagePath, "wb")
    for char in data:
            hexChar = binascii.unhexlify(char)
            emoji.write(hexChar)
    emoji.close

for ligatureSetXml in ttx.find('GSUB').find('LookupList').find('Lookup').find('LigatureSubst'):
    ligatureSet = ligatureSetXml.attrib['glyph'].lower().replace('uni', 'u')
    if ligatureSet.startswith('u'): #TODO: parse missing emojis
        for ligatureXml in ligatureSetXml:
            component = ligatureXml.attrib['components'].replace(',', '_').lower().replace('uni', 'u')
            glyph = ligatureXml.attrib['glyph']
            print 'Renaming emoji_' + glyph + '.png to emoji_' + ligatureSet + '_' + component + '.png'
            try:
                os.rename(args.output + '/emoji_' + glyph + '.png', args.output + '/emoji_' + ligatureSet + '_' + component + '.png')
            except:
                print '!! Cannot rename emoji_' + glyph + '.png'
    else:
        cmap = ttx.find('cmap').find('cmap_format_12')
        code = ''
        for mapElement in cmap:
            if mapElement.attrib['name'] == ligatureSet:
                if len(mapElement.attrib['code']) > 4:
                    code = mapElement.attrib['code'].replace('0x', '')
                else:
                    code = mapElement.attrib['code'].replace('0x', '00')
                try:
                    os.rename(args.output + '/emoji_' + ligatureSet + '.png', args.output + '/emoji_u' + code + '.png')
                except:
                    print '!! Cannot rename emoji_' + ligatureSet + '.png'
        else:
            if code == '':
                print 'Ignoring ' + ligatureSet + '...'

emojis = glob.glob(args.output + './*.png')
for emoji in emojis:
    if not emoji.startswith(args.output + './emoji_u'):
        emoji = emoji.replace(args.output + './emoji_', '').replace('.png', '')
        print 'Fixing ' + emoji + '...'
        cmap = ttx.find('cmap').find('cmap_format_12')
        code = ''
        for mapElement in cmap:
            if mapElement.attrib['name'].lower() == emoji:
                if len(mapElement.attrib['code']) > 4:
                    code = mapElement.attrib['code'].replace('0x', '')
                else:
                    code = mapElement.attrib['code'].replace('0x', '00')
                try:
                    os.rename(args.output + '/emoji_' + emoji + '.png', args.output + '/emoji_u' + code + '.png')
                except:
                    print '!! Cannot fix emoji_' + emoji + '.png'
