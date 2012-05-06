!transformation

!begin []
<|missing-commands|>node{
  source.descendant[?commandset]
  }
;

command [source[?commandnames].descendant[?command].@id == self.@cmdref]
!replace
;

commandgroup|commandset !up [^contents]
!replace
;

commandgroupref[]
!replace
;

!begin !up []
!add
("There are " || count(target.descendant[?command]) || " command(s) missing in " || source:1.sourcename || ".\n").echo
;
