#!/usr/bin/env python

def good_addr(class_A, class_B, class_C):
    # class A
    if class_A == 0 or class_A == 10 or class_A == 127 or (class_A >= 224 and class_A <= 255):
        return 0
    # class B
    if (class_A == 169 and class_B == 254) or (class_A == 172 and (class_B >= 16 and class_B <= 31)) or \
       (class_A == 192 and class_B == 168) or (class_A == 198 and (class_B == 18 or class_B == 19)):
        return 0
    # class C, D, E
    if (class_A == 192 and class_B == 0 and class_C == 0) or \
       (class_A == 192 and class_B == 2 and class_C == 0) or \
       (class_A == 192 and class_B == 88 and class_C == 99) or \
       (class_A == 198 and class_B == 51 and class_C == 100) or \
       (class_A == 203 and class_B == 0 and class_C == 113):
        return 0

    return 1
