const fs=require("fs");
const sharp=require("sharp");
const pngToIco=require("png-to-ico");
(async()=>{
  const svg=fs.readFileSync(__dirname+"/phone-logo.svg");
  const png=await sharp(svg).resize(256,256).png().toBuffer();
  fs.writeFileSync(__dirname+"/icon-256.png",png);
  const ico=await pngToIco([png]);
  fs.writeFileSync(__dirname+"/icon.ico",ico);
})();
