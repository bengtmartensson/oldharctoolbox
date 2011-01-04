!transformation

!begin []
<|html|>node{
  <|head|>node{
    <|meta,  @content := "text/html; charset=ISO-8859-1",
	@http-equiv := "content-type"|>node,
	<|title|>node{"Macro Descriptions"}
  },
  <|body|>node{
	<|h1|>node{"Macro Descriptions"},
	    source.child[?description],
	      <|h2|>node{"Powermap"},
	      <|a, @href := source.@powermap|>node{"XML"},
	      <|a, @href := source.@powermap.sed("/xml$/html/")|>node{"HTML"},
	    source.child[?macros & @visibility === "public"]
	 }
};

macros [self.r.child[?menu]]
!replace 
<|h2|>node{self.r.@name},
  self.r.child[?description],
    <|div, @class := "menufolders"|>node{
      self.r.child[?menu]
    }
;

macros [self.parent[?body]]
!replace 
<|h2|>node{self.r.@name},
  self.r.child[?description],
    <|div, @class := "secondlevelfolders"|>node{
      self.r.child[?macros & @visibility === "public"],
      <|table, @border := "1"|>node{
	<|tbody|>node{self.r.child[?macro]}
      }
    }
;

macros [self.parent[@class === "secondlevelfolders"]]
!replace
<|h3|>node{self.r.@name},
  self.r.child[?description],
    <|div, @class := "thirdlevelfolders"|>node{
      self.r.child[?macros & @visibility === "public"],
	<|table, @border := "1"|>node{
	  <|tbody|>node{self.r.child[?macro]}
	}
    }
;

macros [self.parent[@class === "thirdlevelfolders"]]
!replace
<|h3|>node{self.r.@name},
  self.r.child[?description],
    <|div, @class := "forthlevelfolders"|>node{
	<|table, @border := "1"|>node{
	  <|tbody|>node{self.r.child[?macro]}
	}
    }
;

description []
!replace <|span, @style := "font-style: italic"|>node{self.r.data}
;

macro [@visibility === "public"]
!replace 
#ifdef usedl
  <|dt|>node{self.r.@name},
  <|dd|>node{self.r.child[?description].data}
#else
<|tr, @id := self.r.@name|>node{
  <|th|>node{self.r.@name},
  <|td|>node{self.r.child[?description].data}
}
#endif
;
[]
!replace
;

menu []
!replace
<|h3|>node{"Menu: " || self.@name,
"  ",
self.r.child[?description]
},
<|ol, @class := "menulist"|>node{
  self.r.child[?macrocall]
};

macrocall []
!replace
<|li|>node{
  <|a, @href := "#" || self.@macro, @title := source.descendant[?macro & @name == self.@macro].child[?description].data|>node{self.@macro}
};

!annotation

html []
   _sgml := "prefix:<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\">\n"
;

meta|link|img|hr|br []
        _sgml:="NoEndtag";
