/*
 *  Copyright (C) 2010-2013 JPEXS
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.helpers;

import com.jpexs.decompiler.flash.Configuration;
import static com.jpexs.decompiler.flash.helpers.HilightType.CLASS;
import static com.jpexs.decompiler.flash.helpers.HilightType.SPECIAL;
import com.jpexs.decompiler.flash.helpers.hilight.Highlighting;
import com.jpexs.decompiler.graph.GraphSourceItem;
import com.jpexs.helpers.Helper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Provides methods for highlighting positions of instructions in the text.
 *
 * @author JPEXS
 */
public class HilightedTextWriter extends GraphTextWriter {
    
    public static final String INDENT_STRING = "   ";
    private StringBuilder sb = new StringBuilder();
    private boolean hilight;
    private boolean newLine = true;
    private int indent = 0;
    private Stack<GraphSourceItemPosition> offsets = new Stack<>();
    private Stack<Boolean> stringAddedStack = new Stack<>();
    private boolean stringAdded = false;
    private long startTime;
    private boolean toStringCalled = false;
    private int newLineCount = 0;

    private Stack<Highlighting> hilightStack = new Stack<>();
    public List<Highlighting> traitHilights = new ArrayList<>();
    public List<Highlighting> classHilights = new ArrayList<>();
    public List<Highlighting> methodHilights = new ArrayList<>();
    public List<Highlighting> instructionHilights = new ArrayList<>();
    public List<Highlighting> specialHilights = new ArrayList<>();
    
    public HilightedTextWriter(boolean hilight) {
        startTime = System.currentTimeMillis();
        this.hilight = hilight;
    }

    public HilightedTextWriter(boolean hilight, int indent) {
        startTime = System.currentTimeMillis();
        this.hilight = hilight;
        this.indent = indent;
    }

    @Override
    public boolean getIsHighlighted() {
        return hilight;
    }

    /**
     * Highlights specified text as instruction by adding special tags
     *
     * @param offset Offset of instruction
     * @return HilightedTextWriter
     */
    @Override
    public HilightedTextWriter startOffset(GraphSourceItem src, int pos) {
        GraphSourceItemPosition itemPos = new GraphSourceItemPosition();
        itemPos.graphSourceItem = src;
        itemPos.position = pos;
        offsets.add(itemPos);
        return this;
    }
    
    @Override
    public HilightedTextWriter endOffset() {
        offsets.pop();
        return this;
    }
    
    /**
     * Highlights specified text as method by adding special tags
     *
     * @param index MethodInfo index
     * @return HilightedTextWriter
     */
    @Override
    public HilightedTextWriter startMethod(long index) {
        Map<String, String> data = new HashMap<>();
        data.put("index", Long.toString(index));
        return start(data, HilightType.METHOD);
    }
    
    @Override
    public HilightedTextWriter endMethod() {
        return end(HilightType.METHOD);
    }
    
    /**
     * Highlights specified text as class by adding special tags
     *
     * @param index Class index
     * @return HilightedTextWriter
     */
    @Override
    public HilightedTextWriter startClass(long index) {
        Map<String, String> data = new HashMap<>();
        data.put("index", Long.toString(index));
        return start(data, HilightType.CLASS);
    }
    
    @Override
    public HilightedTextWriter endClass() { 
        return end(HilightType.CLASS);
    }
    
    /**
     * Highlights specified text as trait by adding special tags
     *
     * @param index Trait index
     * @return HilightedTextWriter
     */
    @Override
    public HilightedTextWriter startTrait(long index) {
        Map<String, String> data = new HashMap<>();
        data.put("index", Long.toString(index));
        return start(data, HilightType.TRAIT);
    }
    
    @Override
    public HilightedTextWriter endTrait() {
        return end(HilightType.TRAIT);
    }
    
    @Override
    public HilightedTextWriter hilightSpecial(String text, String type) {
        return hilightSpecial(text, type, 0);
    }
    
    @Override
    public HilightedTextWriter hilightSpecial(String text, String type, int index) {
        Map<String, String> data = new HashMap<>();
        data.put("subtype", type);
        data.put("index", Long.toString(index));
        start(data, HilightType.SPECIAL);
        appendNoHilight(text);
        return end(HilightType.SPECIAL);
    }
    
    @Override
    public HilightedTextWriter append(String str) {
        GraphSourceItemPosition itemPos = offsets.peek();
        GraphSourceItem src = itemPos.graphSourceItem;
        int pos = itemPos.position;
        Highlighting h = null;
        if (src != null && hilight) {
            Map<String, String> data = new HashMap<>();
            data.put("offset", Long.toString(src.getOffset() + pos + 1));
            h = new Highlighting(sb.length() - newLineCount, data, HilightType.OFFSET, str);
            instructionHilights.add(h);
        }
        appendToSb(str);
        if (h != null) {
            h.len = sb.length() - newLineCount - h.startPos;
        }
        return this;
    }

    @Override
    public HilightedTextWriter append(String str, long offset) {
        Highlighting h = null;
        if (hilight) {
            Map<String, String> data = new HashMap<>();
            data.put("offset", Long.toString(offset));
            h = new Highlighting(sb.length() - newLineCount, data, HilightType.OFFSET, str);
            instructionHilights.add(h);
        }
        appendToSb(str);
        if (h != null) {
            h.len = sb.length() - newLineCount - h.startPos;
        }
        return this;
    }

    @Override
    public HilightedTextWriter appendNoHilight(int i) {
        appendNoHilight(Integer.toString(i));
        return this;
    }

    @Override
    public HilightedTextWriter appendNoHilight(String str) {
        appendToSb(str);
        return this;
    }

    @Override
    public HilightedTextWriter indent() {
        indent++;
        return this;
    }

    @Override
    public HilightedTextWriter unindent() {
        indent--;
        return this;
    }

    @Override
    public HilightedTextWriter newLine() {
        appendToSb("\r\n");
        newLine = true;
        newLineCount++;
        return this;
    }

    @Override
    public HilightedTextWriter stripSemicolon() {
        // hack
        if (sb.charAt(sb.length() - 1) == ';') {
            sb.setLength(sb.length() - 1);
        }
        return this;
    }
    
    @Override
    public void setLength(int length) {
        if (length >= sb.length()) {
            return;
        }
        
        String remove = sb.substring(length);
        int removedNewLines = 0;
        for (int i = 0; i < remove.length(); i++) {
            if (remove.charAt(i) == '\n') {
                removedNewLines++;
            }
        }
        newLineCount -= removedNewLines;
        
        sb.setLength(length);
    }
    
    @Override
    public int getLength() {
        return sb.length();
    }
    
    @Override
    public int getIndent() {
        return indent;
    }
    
    @Override
    public String toString() {
        if (toStringCalled) {
            throw new Error("HilightedTextWriter.toString() was already called.");
        }
        if (Configuration.debugMode) {
            long stopTime = System.currentTimeMillis();
            long time = stopTime - startTime;
            if (time > 500) {
                System.out.println("Rendering is too slow: " + Helper.formatTimeSec(time) + " length: " + sb.length());
            }
        }
        toStringCalled = true;
        return sb.toString();
    }
    
    @Override
    public void mark() {
        stringAddedStack.add(stringAdded);
        stringAdded = false;
    }
    
    @Override
    public boolean getMark() {
        boolean result = stringAdded;
        stringAdded = stringAddedStack.pop() || result;
        return result;
    }
    
    private HilightedTextWriter start(Map<String, String> data, HilightType type) {
        if (hilight) {
            Highlighting h = new Highlighting(sb.length() - newLineCount, data, type, null);
            hilightStack.add(h);
        }
        return this;
    }
    
    private HilightedTextWriter end(HilightType expectedType) {
        if (hilight) {
            Highlighting h = hilightStack.pop();
            h.len = sb.length() - newLineCount - h.startPos;
            
            if (!expectedType.equals(h.type)) {
                throw new Error("Hilighting mismatch.");
            }
            
            switch (h.type) {
                case CLASS:
                    classHilights.add(h);
                    break;
                case METHOD:
                    methodHilights.add(h);
                    break;
                case TRAIT:
                    traitHilights.add(h);
                    break;
                case SPECIAL:
                    specialHilights.add(h);
                    break;
                case OFFSET:
                    instructionHilights.add(h);
                    break;
            }
        }
        return this;
    }
    
    private void appendToSb(String str) {
        if (newLine) {
            newLine = false;
            appendIndent();
        }
        sb.append(str);
        stringAdded = true;
    }
    
    private void appendIndent() {
        for (int i = 0; i < indent; i++) {
            appendNoHilight(INDENT_STRING);
        }
    }
}
